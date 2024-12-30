package net.reldo.taskstracker;

import com.google.gson.Gson;
import com.google.inject.Binder;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.reldo.taskstracker.data.Export;
import net.reldo.taskstracker.data.LongSerializer;
import net.reldo.taskstracker.data.TasksSummary;
import net.reldo.taskstracker.data.TrackerConfigStore;
import net.reldo.taskstracker.data.jsondatastore.reader.DataStoreReader;
import net.reldo.taskstracker.data.jsondatastore.reader.HttpDataStoreReader;
import net.reldo.taskstracker.data.reldo.ReldoImport;
import net.reldo.taskstracker.data.task.TaskFromStruct;
import net.reldo.taskstracker.data.task.TaskService;
import net.reldo.taskstracker.data.task.TaskType;
import net.reldo.taskstracker.data.task.filters.FilterService;
import net.reldo.taskstracker.panel.TasksTrackerPluginPanel;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Experience;
import net.runelite.api.GameState;
import net.runelite.api.Skill;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.RuneScapeProfileType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.ProfileChanged;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import net.runelite.client.util.LinkBrowser;

@Slf4j
@PluginDescriptor(
	name = "Tasks Tracker"
)
public class TasksTrackerPlugin extends Plugin
{
	public static final String CONFIG_GROUP_NAME = "tasks-tracker";

	public int[] playerSkills;

	public String taskTextFilter;

	public TasksTrackerPluginPanel pluginPanel;

	private static final long VARP_UPDATE_THROTTLE_DELAY_MS = 7 * 1000;

	private boolean forceUpdateVarpsFlag = false;
	private Set<Integer> varpIdsToUpdate = new HashSet<>();
	private long lastVarpUpdate = 0;
	private NavigationButton navButton;
	private RuneScapeProfileType currentProfileType;
	private final Map<Skill, Integer> oldExperience = new EnumMap<>(Skill.class);

	@Inject	@Named("runelite.version") private String runeliteVersion;
	@Inject private Gson gson;
	@Inject	private Client client;
	@Inject	private SpriteManager spriteManager;
	@Inject	private PluginManager pluginManager;
	@Inject	private ClientToolbar clientToolbar;
	@Inject	private ClientThread clientThread;
	@Inject	private ChatMessageManager chatMessageManager;
	@Getter	@Inject	private ConfigManager configManager;
	@Getter @Inject	private TasksTrackerConfig config;

	@Inject	private TrackerConfigStore trackerConfigStore;
	@Inject private TaskService taskService;
	@Inject private FilterService filterService;

	@Override
	public void configure(Binder binder)
	{
		binder.bind(DataStoreReader.class).to(HttpDataStoreReader.class);
		super.configure(binder);
	}

	@Provides
	TasksTrackerConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TasksTrackerConfig.class);
	}

	@Override
	protected void startUp()
	{
		try
		{
			String taskTypeJsonName = config.taskTypeJsonName();
			taskService.setTaskType(taskTypeJsonName);
		}
		catch (Exception ex)
		{
			log.error("error setting task type in startUp", ex);
		}

		forceUpdateVarpsFlag = false;

		pluginPanel = new TasksTrackerPluginPanel(this, config, spriteManager, taskService);

		boolean isLoggedIn = isLoggedInState(client.getGameState());
		pluginPanel.setLoggedIn(isLoggedIn);
		if (isLoggedIn)
		{
			forceUpdateVarpsFlag = true;
		}

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "panel_icon.png");
		navButton = NavigationButton.builder()
			.tooltip("Task Tracker")
			.icon(icon)
			.priority(5)
			.panel(pluginPanel)
			.build();
		clientToolbar.addNavigation(navButton);

		log.info("Tasks Tracker started!");
	}

	@Override
	protected void shutDown()
	{
		pluginPanel = null;
		taskService.clearTaskTypes();
		clientToolbar.removeNavigation(navButton);
		log.info("Tasks Tracker stopped!");
	}

	@Subscribe
	public void onCommandExecuted(CommandExecuted commandExecuted)
	{
		if (!commandExecuted.getCommand().startsWith("tt")) return;

		if (commandExecuted.getCommand().equalsIgnoreCase("tt-process-varp"))
		{
			String[] args = commandExecuted.getArguments();
			if (args.length == 0) return;

			try
			{
				int varpId = Integer.parseInt(args[0]);
				log.debug("Processing varpId " + varpId);
				processVarpAndUpdateTasks(varpId);
			}
			catch (NumberFormatException e)
			{
				log.debug("Invalid varpId, provide a valid integer");
			}
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged varbitChanged)
	{
		if (forceUpdateVarpsFlag || taskService.isTaskTypeChanged())
		{
			// Force update is coming on next game tick, so ignore varbit change events
			return;
		}
		int varpId = varbitChanged.getVarpId();
		if (!taskService.isVarpInCurrentTaskType(varpId))
		{
			return;
		}
		varpIdsToUpdate.add(varbitChanged.getVarpId());
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged configChanged)
	{
		if (!configChanged.getGroup().equals(CONFIG_GROUP_NAME))
		{
			return;
		}

		log.debug("onConfigChanged {} {}", configChanged.getKey(), configChanged.getNewValue());
		if (configChanged.getKey().equals("untrackUponCompletion") && config.untrackUponCompletion())
		{
			forceVarpUpdate();
		}

		if (configChanged.getKey().equals("filterPanelCollapsible"))
		{
			SwingUtilities.invokeLater(pluginPanel::redraw);
		}

		if (configChanged.getKey().startsWith("tab")) // task list tab config items all start 'tab#'
		{
			pluginPanel.refreshFilterButtonsFromConfig(config.taskListTab());
			refresh();
		}

		if (configChanged.getKey().equals("taskPanelBatchSize"))
		{
			pluginPanel.taskListPanel.setBatchSize(config.taskPanelBatchSize());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		log.debug("onGameStateChanged {}", gameStateChanged.getGameState().toString());
		GameState newGameState = gameStateChanged.getGameState();
		RuneScapeProfileType newProfileType = RuneScapeProfileType.getCurrent(client);

		SwingUtilities.invokeLater(() -> pluginPanel.setLoggedIn(isLoggedInState(newGameState)));

		// Logged in
		if (newGameState == GameState.LOGGING_IN)
		{
			forceUpdateVarpsFlag = true;
		}
		// Changed game mode
		if (isLoggedInState(newGameState) && currentProfileType != null && currentProfileType != newProfileType)
		{
			forceUpdateVarpsFlag = true;
		}

		currentProfileType = newProfileType;
	}

	private boolean isLoggedInState(GameState gameState)
	{
		return gameState == GameState.LOGGED_IN || gameState == GameState.HOPPING || gameState == GameState.LOADING;
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		if (forceUpdateVarpsFlag || taskService.isTaskTypeChanged())
		{
			log.debug("forceUpdateVarpsFlag game tick");
			trackerConfigStore.loadCurrentTaskTypeFromConfig();
			forceVarpUpdate();
			SwingUtilities.invokeLater(() -> pluginPanel.drawNewTaskType());
			forceUpdateVarpsFlag = false;
			taskService.setTaskTypeChanged(false);
		}

		// Flush throttled varp updates
		long currentTimeEpoch = System.currentTimeMillis();
		if (currentTimeEpoch - lastVarpUpdate > VARP_UPDATE_THROTTLE_DELAY_MS)
		{
			flushVarpUpdates(varpIdsToUpdate);
			varpIdsToUpdate = new HashSet<>();
			lastVarpUpdate = currentTimeEpoch;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged statChanged)
	{
		// @todo deprecate one of these, we don't need to track player skills twice.
		// Cache current player skills
		int[] newSkills = client.getRealSkillLevels();
		boolean changed = !Arrays.equals(playerSkills, newSkills);
		if (changed)
		{
			playerSkills = client.getRealSkillLevels();
		}

		final Skill skill = statChanged.getSkill();

		// Modified from m0bilebtw's modification from Nightfirecat's virtual level ups plugin
		final int xpAfter = client.getSkillExperience(skill);
		final int levelAfter = Experience.getLevelForXp(xpAfter);
		final int xpBefore = oldExperience.getOrDefault(skill, -1);
		final int levelBefore = xpBefore == -1 ? -1 : Experience.getLevelForXp(xpBefore);

		oldExperience.put(skill, xpAfter);

		// Do not proceed if any of the following are true:
		//  * xpBefore == -1              (don't fire when first setting new known value)
		//  * xpAfter <= xpBefore         (do not allow 200m -> 200m exp drops)
		//  * levelBefore >= levelAfter   (stop if we're not actually reaching a new level)
		//  * levelAfter > MAX_REAL_LEVEL (stop if above 99)
		if (xpBefore == -1 || xpAfter <= xpBefore || levelBefore >= levelAfter || levelAfter > Experience.MAX_REAL_LEVEL)
		{
			return;
		}

		// If we get here, 'skill' was leveled up!
		SwingUtilities.invokeLater(() -> pluginPanel.taskListPanel.refreshTaskPanelsWithSkill(skill));
	}

	@Subscribe
	public void onProfileChanged(ProfileChanged profileChanged)
	{
		final Optional<Plugin> taskTrackerPlugin = pluginManager.getPlugins().stream().filter(p -> p.getName().equals("Tasks Tracker")).findFirst();
		if (taskTrackerPlugin.isPresent() && pluginManager.isPluginEnabled(taskTrackerPlugin.get()))
		{
			reloadTaskType();
		}
	}

	public void refresh()
	{
		SwingUtilities.invokeLater(() -> pluginPanel.refresh(null));
	}

    public void reloadTaskType() {
        taskService.clearTaskTypes();
        filterService.clearFilterConfigs();
        try {
            String taskTypeJsonName = config.taskTypeJsonName();
            taskService.setTaskType(taskTypeJsonName).thenAccept(isSet -> {
                if (!isSet) {
                    return;
                }
                SwingUtilities.invokeLater(() ->
                {
			pluginPanel.drawNewTaskType();
			pluginPanel.refreshFilterButtonsFromConfig(config.taskListTab());
			pluginPanel.refresh(null);
                });
            });
        } catch (Exception ex) {
            log.error("error setting task type in reload", ex);
        }

    }

	public void saveCurrentTaskTypeData()
	{
		log.debug("saveCurrentTaskTypeData");
		trackerConfigStore.saveCurrentTaskTypeData();
	}

	public void openImportJsonDialog()
	{
		JOptionPane optionPane = new JOptionPane("Paste import data into the text field below to import task tracker data.", JOptionPane.INFORMATION_MESSAGE);
		optionPane.setWantsInput(true);
		JDialog inputDialog = optionPane.createDialog(this.pluginPanel, "Import Tasks Input");
		inputDialog.setAlwaysOnTop(true);
		inputDialog.setVisible(true);

		if (optionPane.getInputValue().equals("") || optionPane.getInputValue().equals("uninitializedValue"))
		{
			this.showMessageBox("Import Tasks Error", "Input was empty so no data has been imported.", JOptionPane.ERROR_MESSAGE, false);
			return;
		}

		String json = "";
		ReldoImport reldoImport;
		try
		{
			json = (String) optionPane.getInputValue();
			reldoImport = this.gson.fromJson(json, ReldoImport.class);
		}
		catch (Exception ex)
		{
			this.showMessageBox("Import Tasks Error", "There was an issue importing task tracker data. " + ex.getMessage(), JOptionPane.ERROR_MESSAGE, false);
			log.error("There was an issue importing task tracker data.", ex);
			log.debug("reldoImport json: {}", json);
			return;
		}

		if (!reldoImport.taskTypeName.equalsIgnoreCase(config.taskTypeJsonName()))
		{
			this.showMessageBox("Import Tasks Error", String.format("Wrong task type. Select the %s task type to import this data.", reldoImport.taskTypeName), JOptionPane.ERROR_MESSAGE, false);
			return;
		}

		optionPane = new JOptionPane("Importing tasks will overwrite task tracker settings and cannot be undone. Are you sure you want to import tasks?", JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION);
		JDialog confirmDialog = optionPane.createDialog(this.pluginPanel, "Import Tasks Overwrite Confirmation");
		confirmDialog.setAlwaysOnTop(true);
		confirmDialog.setVisible(true);

		Object selectedValue = optionPane.getValue();
		if (selectedValue == null)
		{
			return;
		}

		if (selectedValue.equals(JOptionPane.YES_OPTION))
		{
			HashMap<Integer, TaskFromStruct> tasksById = new HashMap<>();
			taskService.getTasks().forEach((task) -> tasksById.put(task.getIntParam("id"), task));

			reldoImport.getTasks().forEach((id, reldoTaskSave) -> {
				TaskFromStruct task = tasksById.get(id);
				task.loadReldoSave(reldoTaskSave);
			});

			trackerConfigStore.saveCurrentTaskTypeData();
			pluginPanel.redraw();
		}
	}

	public void sendTotalsToChat()
	{
		TasksSummary summary = new TasksSummary(taskService.getTasks());
		int trackedTasks = summary.trackedTasksCount;
		int trackedPoints = summary.trackedTasksPoints;

		final String message = new ChatMessageBuilder()
			.append(Color.BLACK, String.format("Task Tracker - Tracked Tasks: %s | Tracked Points: %s", trackedTasks, trackedPoints))
			.build();

		chatMessageManager.queue(
			QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(message)
				.build());
	}

	public void copyJsonToClipboard()
	{
		clientThread.invokeLater(() -> {
			// Not worried with this complexity on the client thread because it's from an infrequent button press
			String json = getCurrentTaskTypeExportJson();
			final StringSelection stringSelection = new StringSelection(json);
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);

			String message = "Copied " + taskService.getCurrentTaskType().getName() + " data to clipboard!";
			showMessageBox("Data Exported!", message, JOptionPane.INFORMATION_MESSAGE, true);
		});
	}

	private void forceVarpUpdate()
	{
		log.debug("forceVarpUpdate");
		processVarpAndUpdateTasks(null).thenAccept((processed) -> {
			if (processed)
			{
				log.debug("forceVarpUpdate processed complete, saving");
				saveCurrentTaskTypeData();
			}
		});
	}

	private void flushVarpUpdates(Set<Integer> varpIds)
	{
		log.debug("Flushing throttled varp updates {}", varpIds);
		varpIds.forEach((id) -> processVarpAndUpdateTasks(id).thenAccept(processed -> {
			if (processed)
			{
				log.debug("flushVarpUpdates processed complete, saving");
				saveCurrentTaskTypeData();
			}
		}));
	}

	private CompletableFuture<Boolean> processTaskStatus(TaskFromStruct task)
	{
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		clientThread.invoke(() -> {
			int taskId =  task.getIntParam("id");
			int varbitIndex = taskId / 32;
			int bitIndex = taskId % 32;
			try
			{
				int varpId = task.getTaskType().getTaskVarps().get(varbitIndex);
				BigInteger varpValue = BigInteger.valueOf(client.getVarpValue(varpId));
				boolean isTaskCompleted = varpValue.testBit(bitIndex);
				task.setCompleted(isTaskCompleted);
				if (isTaskCompleted && config.untrackUponCompletion())
				{
					task.setTracked(false);
				}
				log.debug("process taskFromStruct {} ({}) {}", task.getStringParam("name"), task.getIntParam("id"), isTaskCompleted);
				SwingUtilities.invokeLater(() -> pluginPanel.refresh(task));
				future.complete(isTaskCompleted);
			}
			catch (Exception ex)
			{
				log.error("Error processing task status {}", taskId, ex);
				future.completeExceptionally(ex);
			}
		});
		return future;
	}

	/**
	 * Update task completion status. If no varpId is specified, it updates all tasks in the current task type
	 * @param varpId varp id to update (optional)
	 * @return An observable that emits true if all tasks were processed
	 */
	private CompletableFuture<Boolean> processVarpAndUpdateTasks(@Nullable Integer varpId)
	{
		log.info("processVarpAndUpdateTasks: " + (varpId != null ? varpId : "all"));

		List<TaskFromStruct> tasks = varpId != null ?
			taskService.getTasksFromVarpId(varpId) :
			taskService.getTasks();

		List<CompletableFuture<Boolean>> taskFutures = new ArrayList<>();
		for (TaskFromStruct task : tasks)
		{
			CompletableFuture<Boolean> taskFuture = processTaskStatus(task);
			taskFutures.add(taskFuture);
		}

		CompletableFuture<Void> allTasksFuture = CompletableFuture.allOf(taskFutures.toArray(new CompletableFuture[0]));
		return allTasksFuture.thenApply(v -> true);
	}

	private String getCurrentTaskTypeExportJson()
	{
		TaskType taskType = taskService.getCurrentTaskType();
		Gson gson = this.gson.newBuilder()
			.excludeFieldsWithoutExposeAnnotation()
			.registerTypeAdapter(float.class, new LongSerializer())
			.create();

		if (taskType == null)
		{
			String error = "Cannot export to JSON; no task type selected.";
			log.error(error);
			return error;
		}
		else
		{
			Export export = new Export(taskType, taskService.getTasks(), runeliteVersion, client);
			return gson.toJson(export);
		}
	}

	private void showMessageBox(final String title, final String message, int messageType, boolean showOpenLeagueTools)
	{
		SwingUtilities.invokeLater(() -> {
			JOptionPane optionPane;
			JDialog dialog;

			if (showOpenLeagueTools)
			{
				String[] options = {"Open OS League Tools", "Ok"};

				optionPane = new JOptionPane(message, messageType, JOptionPane.YES_NO_OPTION, null, options, options[1]);
			}
			else
			{
				optionPane = new JOptionPane(message, messageType);
			}

			dialog = optionPane.createDialog(pluginPanel, title);
			dialog.setAlwaysOnTop(true);
			dialog.setVisible(true);

			Object selectedValue = optionPane.getValue();
			if (selectedValue == null)
			{
				return;
			}

			if (selectedValue.equals("Open OS League Tools"))
			{
				LinkBrowser.browse("https://www.osleague.tools/tracker?open=import&tab=tasks");
			}
		});
	}
}
