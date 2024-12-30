package net.reldo.taskstracker.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import lombok.extern.slf4j.Slf4j;
import net.reldo.taskstracker.TasksTrackerConfig;
import net.reldo.taskstracker.TasksTrackerPlugin;
import net.reldo.taskstracker.config.ConfigValues;
import net.reldo.taskstracker.data.jsondatastore.types.FilterConfig;
import net.reldo.taskstracker.data.jsondatastore.types.FilterType;
import net.reldo.taskstracker.data.task.TaskFromStruct;
import net.reldo.taskstracker.data.task.TaskService;
import net.reldo.taskstracker.data.task.TaskType;
import net.reldo.taskstracker.panel.components.SearchBox;
import net.reldo.taskstracker.panel.components.TriToggleButton;
import net.reldo.taskstracker.panel.filters.ComboItem;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

@Slf4j
public class LoggedInPanel extends JPanel
{
	public TaskListPanel taskListPanel;
	private JComboBox<ComboItem<TaskType>> taskTypeDropdown;

	private final TaskService taskService;
	private final TasksTrackerPlugin plugin;
	private final TasksTrackerConfig config;

	// Filter buttons
	private final TriToggleButton completedFilterBtn = new TriToggleButton();
	private final TriToggleButton trackedFilterBtn = new TriToggleButton();
	private final TriToggleButton ignoredFilterBtn = new TriToggleButton();
	private final JPanel titlePanel = new JPanel();

	// Task list tabs
	private final JPanel tabPane = new JPanel();

	// sub-filter panel
	private SubFilterPanel subFilterPanel;
	private SortPanel sortPanel;
	private final JToggleButton collapseBtn = new JToggleButton();
	private JToggleButton tabOne;
	private JToggleButton tabTwo;
	private JToggleButton tabThree;

	public LoggedInPanel(TasksTrackerPlugin plugin, TasksTrackerConfig config, TaskService taskService)
	{
		super(false);
		this.plugin = plugin;
		this.taskService = taskService;
		this.config = config;

		createPanel();
	}

	@Override
	public Dimension getPreferredSize()
	{
		return getParent().getSize();
	}

	public void drawNewTaskType()
	{
		// taskTypeDropdown may become de-synced after profile change
		String selectedTaskTypeJsonName = taskTypeDropdown.getItemAt(taskTypeDropdown.getSelectedIndex()).getValue().getTaskJsonName();
		if(!selectedTaskTypeJsonName.equals(config.taskTypeJsonName()))
		{
			log.debug("Task type dropdown de-synced, attempting to find current task type");
			for(int i = 0; i < taskTypeDropdown.getItemCount(); i++)
			{
				ComboItem<TaskType> item = taskTypeDropdown.getItemAt(i);
				if(item.getValue().getTaskJsonName().equals(config.taskTypeJsonName()))
				{
					log.debug("Current task type found, setting selected task type");
					taskTypeDropdown.setSelectedIndex(i);
					break;
				}
			}
		}

		subFilterPanel.redraw();
		sortPanel.redraw();
		updateCollapseButtonText();

		taskListPanel.drawNewTaskType();
		refreshFilterButtonsFromConfig(config.taskListTab());
	}

	public void redraw()
	{
		subFilterPanel.redraw();
		sortPanel.redraw();
		updateCollapseButtonText();

		taskListPanel.redraw();
	}

	public void refresh(TaskFromStruct task)
	{
		if(task == null)
		{
			updateCollapseButtonText();
			refreshTabNames();
		}
		taskListPanel.refresh(task);
	}

	private void createPanel()
	{
		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		taskListPanel = new TaskListPanel(plugin, taskService);

		add(getNorthPanel(), BorderLayout.NORTH);
		add(getCenterPanel(), BorderLayout.CENTER);
		add(getSouthPanel(), BorderLayout.SOUTH);
	}

	private JPanel getCenterPanel() {
		// wrapper for the task list and tab buttons
		final JPanel taskListPanel = new JPanel(new BorderLayout());
		taskListPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		taskListPanel.setBorder(new MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR));
		taskListPanel.setAlignmentX(JPanel.CENTER_ALIGNMENT);

		tabPane.setLayout(new BoxLayout(tabPane, BoxLayout.X_AXIS));
		tabPane.setBorder(new EmptyBorder(0,0,0,0));
		tabPane.setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH,24));

		tabOne = tabButton(config.tab1Name(), ConfigValues.TaskListTabs.TAB_ONE);
		tabTwo = tabButton(config.tab2Name(), ConfigValues.TaskListTabs.TAB_TWO);
		tabThree = tabButton(config.tab3Name(), ConfigValues.TaskListTabs.TAB_THREE);

		ButtonGroup tabGroup = new ButtonGroup();

		tabGroup.add(tabOne);
		tabGroup.add(tabTwo);
		tabGroup.add(tabThree);

		tabPane.add(Box.createHorizontalGlue());
		tabPane.add(tabOne);
		tabPane.add(Box.createHorizontalGlue());
		tabPane.add(tabTwo);
		tabPane.add(Box.createHorizontalGlue());
		tabPane.add(tabThree);
		tabPane.add(Box.createHorizontalGlue());

		taskListPanel.add(tabPane, BorderLayout.NORTH);
		taskListPanel.add(this.taskListPanel, BorderLayout.CENTER);

		return taskListPanel;
	}

	public void tabChanged(ConfigValues.TaskListTabs newTab)
	{
		if(newTab != null)
		{
			saveCurrentTabFilters();
			plugin.getConfigManager().setConfiguration(TasksTrackerPlugin.CONFIG_GROUP_NAME, "taskListTab", newTab);
			refreshFilterButtonsFromConfig(newTab);
			plugin.refresh();
		}
	}

	private void saveCurrentTabFilters()
	{
		String tab = "tab" + (config.taskListTab().ordinal() + 1); // tabs are 1-indexed

		Enum configValue;

		if (plugin.getConfigManager().getConfiguration(TasksTrackerPlugin.CONFIG_GROUP_NAME, tab + "CompletedLock").equals("false"))
		{
			configValue = ConfigValues.CompletedFilterValues.values()[completedFilterBtn.getState()];
			plugin.getConfigManager().setConfiguration(TasksTrackerPlugin.CONFIG_GROUP_NAME, tab + "CompletedValue", configValue);
		}

		if (plugin.getConfigManager().getConfiguration(TasksTrackerPlugin.CONFIG_GROUP_NAME, tab + "TrackedLock").equals("false"))
		{
			configValue = ConfigValues.TrackedFilterValues.values()[trackedFilterBtn.getState()];
			plugin.getConfigManager().setConfiguration(TasksTrackerPlugin.CONFIG_GROUP_NAME, tab + "TrackedValue", configValue);
		}

		if (plugin.getConfigManager().getConfiguration(TasksTrackerPlugin.CONFIG_GROUP_NAME, tab + "IgnoredLock").equals("false"))
		{
			configValue = ConfigValues.IgnoredFilterValues.values()[ignoredFilterBtn.getState()];
			plugin.getConfigManager().setConfiguration(TasksTrackerPlugin.CONFIG_GROUP_NAME, tab + "IgnoredValue", configValue);
		}
	}

	// TODO reduce duplication
	public void refreshFilterButtonsFromConfig(ConfigValues.TaskListTabs tab)
	{
		switch (tab)
		{
			case TAB_ONE:
				if(!tabOne.isSelected()) tabOne.setSelected(true);
				trackedFilterBtn.setState(config.tab1TrackedValue().ordinal());
				trackedFilterBtn.setEnabled(!config.tab1TrackedLock());
				completedFilterBtn.setState(config.tab1CompletedValue().ordinal());
				completedFilterBtn.setEnabled(!config.tab1CompletedLock());
				ignoredFilterBtn.setState(config.tab1IgnoredValue().ordinal());
				ignoredFilterBtn.setEnabled(!config.tab1IgnoredLock());
				actionAllFilterButtonsNoRefresh();
				break;
			case TAB_TWO:
				if(!tabTwo.isSelected()) tabTwo.setSelected(true);
				trackedFilterBtn.setState(config.tab2TrackedValue().ordinal());
				trackedFilterBtn.setEnabled(!config.tab2TrackedLock());
				completedFilterBtn.setState(config.tab2CompletedValue().ordinal());
				completedFilterBtn.setEnabled(!config.tab2CompletedLock());
				ignoredFilterBtn.setState(config.tab2IgnoredValue().ordinal());
				ignoredFilterBtn.setEnabled(!config.tab2IgnoredLock());
				actionAllFilterButtonsNoRefresh();
				break;
			case TAB_THREE:
				if(!tabThree.isSelected()) tabThree.setSelected(true);
				trackedFilterBtn.setState(config.tab3TrackedValue().ordinal());
				trackedFilterBtn.setEnabled(!config.tab3TrackedLock());
				completedFilterBtn.setState(config.tab3CompletedValue().ordinal());
				completedFilterBtn.setEnabled(!config.tab3CompletedLock());
				ignoredFilterBtn.setState(config.tab3IgnoredValue().ordinal());
				ignoredFilterBtn.setEnabled(!config.tab3IgnoredLock());
				actionAllFilterButtonsNoRefresh();
				break;
			default:
				break;
		}
	}

	private JToggleButton tabButton(String label, ConfigValues.TaskListTabs tab)
	{
		JToggleButton button = new JToggleButton(label);

		button.setBorder(new EmptyBorder(2,5,2,5));
		button.setBackground(ColorScheme.DARK_GRAY_COLOR);
		button.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		button.addActionListener(e -> tabChanged(tab));

		return button;
	}

	private void refreshTabNames()
	{
		String tabName;
		if (!tabOne.getText().equals(config.tab1Name()))
		{
			tabName = config.tab1Name().isBlank() ? "Tab 1" : config.tab1Name();
			tabOne.setText(tabName);
		}
		if (!tabTwo.getText().equals(config.tab2Name()))
		{
			tabName = config.tab2Name().isBlank() ? "Tab 2" : config.tab2Name();
			tabTwo.setText(tabName);
		}
		if (!tabThree.getText().equals(config.tab3Name()))
		{
			tabName = config.tab3Name().isBlank() ? "Tab 3" : config.tab3Name();
			tabThree.setText(tabName);
		}
	}

	private JPanel getSouthPanel()
	{
		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.setBorder(new EmptyBorder(5,0,2,0));

		JButton importButton = new JButton("Import");
		importButton.setBorder(new EmptyBorder(5, 5, 5, 5));
		importButton.setLayout(new BorderLayout(0, PluginPanel.BORDER_OFFSET));
		importButton.addActionListener(e -> plugin.openImportJsonDialog());
		southPanel.add(importButton, BorderLayout.WEST);

		JButton totalsButton = new JButton("Totals");
		totalsButton.setBorder(new EmptyBorder(5, 5, 5, 5));
		totalsButton.setLayout(new BorderLayout(0, PluginPanel.BORDER_OFFSET));
		totalsButton.addActionListener(e -> plugin.sendTotalsToChat());
		southPanel.add(totalsButton, BorderLayout.CENTER);

		JButton exportButton = new JButton("Export");
		exportButton.setBorder(new EmptyBorder(5, 5, 5, 5));
		exportButton.setLayout(new BorderLayout(0, PluginPanel.BORDER_OFFSET));
		exportButton.addActionListener(e -> plugin.copyJsonToClipboard());
		southPanel.add(exportButton, BorderLayout.EAST);

		return southPanel;
	}

	private JPanel getNorthPanel()
	{
		JPanel northPanel = new JPanel();
		BoxLayout layout = new BoxLayout(northPanel, BoxLayout.Y_AXIS);
		northPanel.setLayout(layout);
		northPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

		ArrayList<ComboItem<TaskType>> taskTypeItems = new ArrayList<>();
		taskService.getTaskTypesByJsonName().forEach((taskTypeJsonName, taskType) -> taskTypeItems.add(new ComboItem(taskType, taskType.getName())));
		ComboItem<TaskType>[] comboItemsArray = taskTypeItems.toArray(new ComboItem[0]);
		taskTypeDropdown = new JComboBox<>(comboItemsArray);
		taskTypeDropdown.setAlignmentX(LEFT_ALIGNMENT);

        TaskType currentTaskType = taskService.getCurrentTaskType();
        ComboItem<TaskType> currentTaskTypeComboItem = Arrays.stream(comboItemsArray)
                .filter(item -> item.getValue().equals(currentTaskType))
                .findFirst().orElseGet(() -> comboItemsArray[0]);
        taskTypeDropdown.setSelectedItem(currentTaskTypeComboItem);
        taskTypeDropdown.addActionListener(e -> {
            TaskType taskType = taskTypeDropdown.getItemAt(taskTypeDropdown.getSelectedIndex()).getValue();
            taskService.setTaskType(taskType).thenAccept(wasTaskTypeChanged -> {
                if (wasTaskTypeChanged) {
					SwingUtilities.invokeLater(() ->
					{
						refresh(null);
					});
                }
            });
        });
        taskTypeDropdown.setFocusable(false);

		// Wrapper for collapsible sub-filter menu
		JPanel subFilterWrapper = new JPanel();
		subFilterWrapper.setLayout(new BorderLayout());
		subFilterWrapper.setBorder(new MatteBorder(1, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR));
		subFilterWrapper.setAlignmentX(LEFT_ALIGNMENT);
		subFilterWrapper.setBackground(ColorScheme.DARKER_GRAY_COLOR);

		// collapse button
		SwingUtil.removeButtonDecorations(collapseBtn);
		collapseBtn.setIcon(Icons.MENU_COLLAPSED_ICON);
		collapseBtn.setSelectedIcon(Icons.MENU_EXPANDED_ICON);
		collapseBtn.setRolloverIcon(Icons.MENU_ICON_HOVER);
		SwingUtil.addModalTooltip(collapseBtn, "Collapse filters", "Expand filters");
		collapseBtn.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		collapseBtn.setAlignmentX(LEFT_ALIGNMENT);
		collapseBtn.setUI(new BasicButtonUI()); // substance breaks the layout
		collapseBtn.addActionListener(ev -> subFilterPanel.setVisible(!subFilterPanel.isVisible()));
		collapseBtn.setHorizontalTextPosition(JButton.CENTER);
		collapseBtn.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		collapseBtn.setFont(FontManager.getRunescapeSmallFont());
		collapseBtn.setBorder(new EmptyBorder(2, 0, 2, 0));
		collapseBtn.setFocusable(false);

		// panel to hold sub-filters
		subFilterPanel = new SubFilterPanel(plugin, taskService);

		subFilterWrapper.add(collapseBtn, BorderLayout.NORTH);
		subFilterWrapper.add(subFilterPanel, BorderLayout.CENTER);

		sortPanel = new SortPanel(plugin, taskService, taskListPanel);

		northPanel.add(getTitleAndButtonPanel());
		northPanel.add(Box.createVerticalStrut(10));
		northPanel.add(taskTypeDropdown);
		northPanel.add(Box.createVerticalStrut(2));
		northPanel.add(getSearchPanel());
		northPanel.add(Box.createVerticalStrut(2));
		northPanel.add(sortPanel);
		northPanel.add(Box.createVerticalStrut(2));
		northPanel.add(subFilterWrapper);

		return northPanel;
	}

	private JPanel getTitleAndButtonPanel()
	{
		titlePanel.setLayout(new BorderLayout());
		titlePanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		titlePanel.setPreferredSize(new Dimension(0, 30));
		titlePanel.setBorder(new EmptyBorder(5, 5, 5, 10));

		JLabel title = new JLabel("Tasks Tracker");
		title.setHorizontalAlignment(SwingConstants.LEFT);
		title.setForeground(Color.WHITE);

		JPopupMenu reloadPluginPopup = new JPopupMenu();
		reloadPluginPopup.setBorder(new EmptyBorder(5, 5, 5, 5));
		JMenuItem reloadPluginMenuItem = new JMenuItem("Reload plugin");
		reloadPluginMenuItem.addActionListener(e -> plugin.reloadTaskType());
		reloadPluginPopup.add(reloadPluginMenuItem);
		title.setComponentPopupMenu(reloadPluginPopup);

		// Filter button bar
		final JPanel viewControls = new JPanel();
		viewControls.setLayout(new BoxLayout(viewControls, BoxLayout.X_AXIS));
		viewControls.setBackground(ColorScheme.DARK_GRAY_COLOR);

		// Completed tasks filter button
		SwingUtil.removeButtonDecorations(completedFilterBtn);
		completedFilterBtn.setIcons(Icons.COMPLETE_INCOMPLETE_ICON, Icons.COMPLETE_ONLY_ICON, Icons.INCOMPLETE_ONLY_ICON);
		completedFilterBtn.setToolTips("All tasks", "Completed tasks only", "Incomplete tasks only");
		completedFilterBtn.setBackground(ColorScheme.DARK_GRAY_COLOR);
		completedFilterBtn.setStateChangedAction(e -> filterButtonAction("completed"));
		completedFilterBtn.popupMenuEnabled(true);
		completedFilterBtn.setState(config.completedFilter().ordinal());

		viewControls.add(completedFilterBtn);

		// Tracked tasks filter button
		SwingUtil.removeButtonDecorations(trackedFilterBtn);
		trackedFilterBtn.setIcons(Icons.TRACKED_UNTRACKED_ICON, Icons.TRACKED_ONLY_ICON, Icons.UNTRACKED_ONLY_ICON);
		trackedFilterBtn.setToolTips("All tasks", "Tracked tasks only", "Untracked tasks only");
		trackedFilterBtn.setBackground(ColorScheme.DARK_GRAY_COLOR);
		trackedFilterBtn.setStateChangedAction(e -> filterButtonAction("tracked"));
		trackedFilterBtn.popupMenuEnabled(true);
		trackedFilterBtn.setState(config.trackedFilter().ordinal());

		viewControls.add(trackedFilterBtn);

		// Ignored tasks filter button
		SwingUtil.removeButtonDecorations(ignoredFilterBtn);
		ignoredFilterBtn.setIcons(Icons.UNIGNORED_ONLY_ICON, Icons.IGNORED_UNIGNORED_ICON, Icons.IGNORED_ONLY_ICON);
		ignoredFilterBtn.setToolTips("Hide ignored tasks", "All tasks", "Ignored tasks only");
		ignoredFilterBtn.setBackground(ColorScheme.DARK_GRAY_COLOR);
		ignoredFilterBtn.setStateChangedAction(e -> filterButtonAction("ignored"));
		ignoredFilterBtn.popupMenuEnabled(true);
		ignoredFilterBtn.setState(config.ignoredFilter().ordinal());

		viewControls.add(ignoredFilterBtn);

		titlePanel.add(viewControls, BorderLayout.EAST);
		titlePanel.add(title, BorderLayout.WEST);
		titlePanel.setAlignmentX(LEFT_ALIGNMENT);

		return titlePanel;
	}

	private void filterButtonActionNoRefresh(String filter)
	{
		int state;
		Enum configValue;

		switch (filter)
		{
			case "completed":
				state = completedFilterBtn.getState();
				configValue = ConfigValues.CompletedFilterValues.values()[state];
				break;
			case "tracked":
				state = trackedFilterBtn.getState();
				configValue = ConfigValues.TrackedFilterValues.values()[state];
				break;
			case "ignored":
				state = ignoredFilterBtn.getState();
				configValue = ConfigValues.IgnoredFilterValues.values()[state];
				break;
			default:
				log.debug("Filter button action failed due to unrecognised filter.");
				return;
		}

		plugin.getConfigManager().setConfiguration(TasksTrackerPlugin.CONFIG_GROUP_NAME, filter + "Filter", configValue);
	}

	private void filterButtonAction(String filter)
	{
		filterButtonActionNoRefresh(filter);
		plugin.refresh();
	}

	private void actionAllFilterButtonsNoRefresh()
	{
		filterButtonActionNoRefresh("tracked");
		filterButtonActionNoRefresh("ignored");
		filterButtonActionNoRefresh("completed");
	}

	private void actionAllFilterButtons()
	{
		actionAllFilterButtonsNoRefresh();
		plugin.refresh();
	}

	private JPanel getSearchPanel()
	{
		JPanel filtersPanel = new JPanel();
		filtersPanel.setAlignmentX(LEFT_ALIGNMENT);
		filtersPanel.setLayout(new BoxLayout(filtersPanel, BoxLayout.Y_AXIS));

		SearchBox textSearch = new SearchBox();
		textSearch.addTextChangedListener(() -> {
			plugin.taskTextFilter = textSearch.getText().toLowerCase();
			plugin.refresh();
		});

		filtersPanel.add(textSearch);

		return filtersPanel;
	}

	private void updateCollapseButtonText()
	{
		if (taskService.getCurrentTaskType() == null) return;
		
		ArrayList<FilterConfig> filters = taskService.getCurrentTaskType().getFilters();

		int countInclusive = 0;
		int countExclusive = 0;

		for (FilterConfig filterConfig : filters)
		{
			String filterText = Optional.ofNullable(plugin.getConfigManager()
					.getConfiguration(TasksTrackerPlugin.CONFIG_GROUP_NAME,
							taskService.getCurrentTaskType().getFilterConfigPrefix() + filterConfig.getConfigKey()))
					.orElse("");

			int count = (filterText.isEmpty() || filterText.equals("-1")) ? 0 : filterText.split(",").length;

			if (filterConfig.getFilterType().equals(FilterType.BUTTON_FILTER)) countInclusive += count;
			if (filterConfig.getFilterType().equals(FilterType.DROPDOWN_FILTER)) countExclusive += count;
		}

		collapseBtn.setText(countInclusive + " inclusive, "  + countExclusive + " exclusive filters");
	}
}