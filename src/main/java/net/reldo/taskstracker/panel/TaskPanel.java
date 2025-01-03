package net.reldo.taskstracker.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.JToolTip;
import javax.swing.ToolTipManager;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.reldo.taskstracker.HtmlUtil;
import net.reldo.taskstracker.TasksTrackerConfig;
import net.reldo.taskstracker.TasksTrackerPlugin;
import net.reldo.taskstracker.config.ConfigValues.CompletedFilterValues;
import net.reldo.taskstracker.config.ConfigValues.IgnoredFilterValues;
import net.reldo.taskstracker.config.ConfigValues.TrackedFilterValues;
import net.reldo.taskstracker.data.jsondatastore.types.FilterType;
import net.reldo.taskstracker.data.jsondatastore.types.TaskDefinitionSkill;
import net.reldo.taskstracker.data.task.TaskFromStruct;
import net.reldo.taskstracker.data.task.filters.Filter;
import net.reldo.taskstracker.data.task.filters.ParamButtonFilter;
import net.reldo.taskstracker.data.task.filters.ParamDropdownFilter;
import net.runelite.api.Constants;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.SwingUtil;

@Slf4j
public class TaskPanel extends JPanel
{
	public final TaskFromStruct task;

	private final JLabel tierIcon = new JLabel();
	private final JPanel container = new JPanel(new BorderLayout());
	private final JPanel body = new JPanel(new BorderLayout());
	private final JLabel name = new JLabel("task");
	private final JLabel description = new JLabel("description");
	private final JPanel buttons = new JPanel();
	private final JToggleButton toggleTrack = new JToggleButton();
	private final JToggleButton toggleIgnore = new JToggleButton();

	protected final ArrayList<Filter> filters = new ArrayList<>();

	protected TasksTrackerPlugin plugin;

	public TaskPanel(TasksTrackerPlugin plugin, TaskFromStruct task)
	{
		super(new BorderLayout());
		this.plugin = plugin;
		this.task = task;
		createPanel();
		setComponentPopupMenu(getPopupMenu());
		ToolTipManager.sharedInstance().registerComponent(this);
		refresh();

		task.getTaskType().getFilters().forEach((filterConfig) -> {
			String paramName = filterConfig.getValueName();
			if (filterConfig.getFilterType().equals(FilterType.BUTTON_FILTER))
			{
				Filter filter = new ParamButtonFilter(plugin.getConfigManager(), paramName, task.getTaskType().getTaskJsonName() + "." + filterConfig.getConfigKey());
				filters.add(filter);
			}
			else if (filterConfig.getFilterType().equals(FilterType.DROPDOWN_FILTER))
			{
				Filter filter = new ParamDropdownFilter(plugin.getConfigManager(), paramName, task.getTaskType().getTaskJsonName() + "." + filterConfig.getConfigKey());
				filters.add(filter);
			}
		});
	}

	public JPopupMenu getPopupMenu()
	{
		return null;
	}

	public String getTaskTooltip()
	{
		StringBuilder tooltipText = new StringBuilder();
		tooltipText.append(HtmlUtil.wrapWithBold(task.getName())).append(HtmlUtil.HTML_LINE_BREAK);
		tooltipText.append(task.getDescription()).append(HtmlUtil.HTML_LINE_BREAK);

		String skillSection = getSkillSectionHtml();
		if (skillSection != null)
		{
			tooltipText.append(skillSection).append(HtmlUtil.HTML_LINE_BREAK);
		}

		String wikiNotes = task.getTaskDefinition().getWikiNotes();
		if (wikiNotes != null && !wikiNotes.isEmpty())
		{
			tooltipText.append(HtmlUtil.HTML_LINE_BREAK).append(wikiNotes).append(HtmlUtil.HTML_LINE_BREAK);
		}

		if (task.isCompleted())
		{
			tooltipText.append(HtmlUtil.HTML_LINE_BREAK);
			String datePattern = "MM-dd-yyyy hh:mma";
			SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);
			tooltipText.append("✔ ").append(simpleDateFormat.format(new Date(task.getCompletedOn())));
		}

		Float completionPercent = task.getTaskDefinition().getCompletionPercent();
		if (completionPercent != null)
		{
			tooltipText.append(HtmlUtil.HTML_LINE_BREAK).append("Players Completed: ").append(completionPercent).append('%');
		}

		String userNotes = task.getNote();
		if (userNotes != null && !userNotes.isEmpty())
		{
			tooltipText.append(HtmlUtil.HTML_LINE_BREAK).append(HtmlUtil.HTML_LINE_BREAK);
			tooltipText.append(HtmlUtil.wrapWithItalics(userNotes)).append(HtmlUtil.HTML_LINE_BREAK);
		}

		return HtmlUtil.wrapWithHtml(
			HtmlUtil.wrapWithWrappingParagraph(tooltipText.toString(), 200)
		);
	}

	public Color getTaskBackgroundColor()
	{
		if (plugin.playerSkills == null)
		{
			return ColorScheme.DARKER_GRAY_COLOR;
		}

		if (task.isCompleted())
		{
			return Colors.COMPLETED_BACKGROUND_COLOR;
		}

		if (task.getTaskDefinition().getSkills() == null || task.getTaskDefinition().getSkills().size() == 0)
		{
			return ColorScheme.DARKER_GRAY_COLOR;
		}

		for (TaskDefinitionSkill requiredSkill : task.getTaskDefinition().getSkills())
		{
			Skill skill;
			String requiredSkillName = requiredSkill.getSkill().toUpperCase();
			try
			{
				skill = Skill.valueOf(requiredSkillName);
			}
			catch (IllegalArgumentException ex)
			{
				log.error("invalid skill name " + requiredSkillName);
				continue;
			}

			if (plugin.playerSkills[skill.ordinal()] < requiredSkill.getLevel())
			{
				return Colors.UNQUALIFIED_BACKGROUND_COLOR;
			}
		}

		return ColorScheme.DARKER_GRAY_COLOR;
	}

	public void createPanel()
	{
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 7, 0));

		container.setBorder(new EmptyBorder(7, 7, 6, 0));

		// Body

		name.setFont(FontManager.getRunescapeSmallFont());
		name.setForeground(Color.WHITE);
		body.add(name, BorderLayout.NORTH);

		description.setFont(FontManager.getRunescapeSmallFont());
		description.setForeground(Color.GRAY);
		body.add(description, BorderLayout.CENTER);

		// Buttons
		buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
		buttons.setBorder(new EmptyBorder(0, 0, 0, 7));

		toggleTrack.setPreferredSize(new Dimension(8, 8));
		toggleTrack.setIcon(Icons.PLUS_ICON);
		toggleTrack.setSelectedIcon(Icons.MINUS_ICON);
		toggleTrack.setBorder(new EmptyBorder(5, 0, 5, 0));
		toggleTrack.addActionListener(e -> {
			task.setTracked(toggleTrack.isSelected());
			plugin.pluginPanel.taskListPanel.refresh(task);
			plugin.saveCurrentTaskTypeData();
		});
		SwingUtil.removeButtonDecorations(toggleTrack);

		toggleIgnore.setPreferredSize(new Dimension(8, 8));
		toggleIgnore.setIcon(Icons.EYE_CROSS_GREY);
		toggleIgnore.setSelectedIcon(Icons.EYE_ICON);
		SwingUtil.addModalTooltip(toggleIgnore, "Unignore", "Ignore");
		toggleIgnore.setBorder(new EmptyBorder(5, 0, 5, 0));
		toggleIgnore.addActionListener(e -> {
			task.setIgnored(!task.isIgnored());
			plugin.pluginPanel.taskListPanel.refresh(task);
			plugin.saveCurrentTaskTypeData();
		});
		SwingUtil.removeButtonDecorations(toggleIgnore);

		buttons.add(toggleTrack);
		buttons.add(toggleIgnore);

		// Full
		container.add(tierIcon, BorderLayout.WEST);
		container.add(body, BorderLayout.CENTER);
		container.add(buttons, BorderLayout.EAST);

		BufferedImage tierSprite = task.getTaskType().getTierSprites().get(task.getTier());
		if (tierSprite != null)
		{
			tierIcon.setMinimumSize(new Dimension(Constants.ITEM_SPRITE_WIDTH, Constants.ITEM_SPRITE_HEIGHT));
			tierIcon.setIcon(new ImageIcon(tierSprite));
			tierIcon.setBorder(new EmptyBorder(0, 0, 0, 5));
		}
		else
		{
			tierIcon.setBorder(new EmptyBorder(0, 0, 0, 0));
		}

		add(container, BorderLayout.NORTH);

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (e.isPopupTrigger())
				{
					JPopupMenu menu = createTaskPopupMenu();
					menu.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
	}

	public JPopupMenu createTaskPopupMenu()
	{
		JPopupMenu popupMenu = new JPopupMenu();
		JMenuItem editNoteItem = new JMenuItem("Edit Note");
		editNoteItem.addActionListener(e -> editTaskNote());
		popupMenu.add(editNoteItem);
		JMenuItem openWikiItem = new JMenuItem("Wiki");
		openWikiItem.addActionListener(e -> openRuneScapeWiki());
		popupMenu.add(openWikiItem);
		return popupMenu;
	}

	private void editTaskNote()
	{
		JOptionPane optionPane = new JOptionPane("Enter the notes to associate with this task (empty removes note):", JOptionPane.INFORMATION_MESSAGE);
		optionPane.setInputValue(task.getNote());
		optionPane.setWantsInput(true);
		JDialog inputDialog = optionPane.createDialog(this, "Set tasks note");
		inputDialog.setAlwaysOnTop(true);
		inputDialog.setVisible(true);
		String note = optionPane.getInputValue().toString();
		task.setNote(note);
		plugin.saveCurrentTaskTypeData();
	}

	private void openRuneScapeWiki()
	{
		String wikiUrl = String.format("https://oldschool.runescape.wiki/%s", URLEncoder.encode(task.getName().replace(' ', '_'), StandardCharsets.UTF_8));
		if (Desktop.isDesktopSupported())
		{
			try
			{
				Desktop.getDesktop().browse(new URI(wikiUrl));
			}
			catch (IOException | URISyntaxException ex)
			{
				ex.printStackTrace();
			}
		}
		else
		{
			log.warn("Desktop browsing is not supported on this system.");
		}
	}

	public void refresh()
	{
		setBackgroundColor(getTaskBackgroundColor());
		name.setText(HtmlUtil.wrapWithHtml(task.getName()));
		description.setText(HtmlUtil.wrapWithHtml(task.getDescription()));
		toggleTrack.setSelected(task.isTracked());
		toggleIgnore.setSelected(task.isIgnored());

		setVisible(meetsFilterCriteria());

		revalidate();
	}

	protected boolean meetsFilterCriteria()
	{
		String nameLowercase = task.getName().toLowerCase();
		String descriptionLowercase = task.getDescription().toLowerCase();
		if (plugin.taskTextFilter != null &&
			!nameLowercase.contains(plugin.taskTextFilter) &&
			!descriptionLowercase.contains(plugin.taskTextFilter))
		{
			return false;
		}

		TasksTrackerConfig config = plugin.getConfig();

		for (Filter filter : filters)
		{
			if (!filter.meetsCriteria(task))
			{
				return false;
			}
		}

		if (config.completedFilter().equals(CompletedFilterValues.INCOMPLETE) && task.isCompleted())
		{
			return false;
		}
		if (config.completedFilter().equals(CompletedFilterValues.COMPLETE) && !task.isCompleted())
		{
			return false;
		}

		if (config.ignoredFilter().equals(IgnoredFilterValues.NOT_IGNORED) && task.isIgnored())
		{
			return false;
		}
		if (config.ignoredFilter().equals(IgnoredFilterValues.IGNORED) && !task.isIgnored())
		{
			return false;
		}

		if (config.trackedFilter().equals(TrackedFilterValues.UNTRACKED) && task.isTracked())
		{
			return false;
		}
		return !config.trackedFilter().equals(TrackedFilterValues.TRACKED) || task.isTracked();
	}

	private void setBackgroundColor(Color color)
	{
		container.setBackground(color);
		body.setBackground(color);
		buttons.setBackground(color);
	}

	@Override
	public Dimension getMaximumSize()
	{
		return new Dimension(PluginPanel.PANEL_WIDTH, getPreferredSize().height);
	}

	@Override
	public JToolTip createToolTip()
	{
		JToolTip customTooltip = new JToolTip();
		customTooltip.setFont(FontManager.getRunescapeSmallFont());
		return customTooltip;
	}

	@Override
	public String getToolTipText(MouseEvent mouseEvent)
	{
		return getTaskTooltip();
	}

	private String getSkillSectionHtml()
	{
		List<TaskDefinitionSkill> requiredSkills = task.getTaskDefinition().getSkills();
		if (requiredSkills == null)
		{
			return null;
		}
		StringBuilder skillSection = new StringBuilder();
		skillSection.append(HtmlUtil.HTML_LINE_BREAK);
		for (TaskDefinitionSkill requiredSkill : requiredSkills)
		{
			Skill skill;
			try
			{
				skill = Skill.valueOf(requiredSkill.getSkill().toUpperCase());
			}
			catch (IllegalArgumentException ex)
			{
				log.warn("unknown skill: {}", requiredSkill.getSkill().toUpperCase(), ex);
				continue;
			}


			Integer requiredLevel = requiredSkill.getLevel();
			int playerLevel = -1;
			if (requiredLevel == null)
			{
				continue;
			}
			if (plugin.playerSkills != null)
			{
				playerLevel = plugin.playerSkills[skill.ordinal()];
			}
			String skillMessage = getSkillRequirementHtml(requiredSkill.getSkill().toLowerCase(), playerLevel, requiredLevel);
			skillSection.append(skillMessage).append(" ");
		}

		return skillSection.toString();
	}

	private String getSkillRequirementHtml(String skillName, Integer playerLevel, int requiredLevel)
	{
		String skillIconPath = "/skill_icons_small/" + skillName + ".png";
		URL url = SkillIconManager.class.getResource(skillIconPath);
		Color color = playerLevel >= requiredLevel ? Colors.QUALIFIED_TEXT_COLOR : Colors.UNQUALIFIED_TEXT_COLOR;
		return HtmlUtil.imageTag(url) + " " + HtmlUtil.colorTag(color, playerLevel + "/" + requiredLevel);
	}

	private String getPointsTooltipText()
	{
		int points = this.task.getPoints();
		if (points == 0)
		{
			return "";
		}
		return " - " + points + " points";
	}
}
