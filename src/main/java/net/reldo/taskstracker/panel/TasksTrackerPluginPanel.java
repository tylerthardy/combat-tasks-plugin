package net.reldo.taskstracker.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.reldo.taskstracker.TasksTrackerConfig;
import net.reldo.taskstracker.TasksTrackerPlugin;
import net.reldo.taskstracker.config.ConfigValues;
import net.reldo.taskstracker.data.task.TaskFromStruct;
import net.reldo.taskstracker.data.task.TaskService;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

@Slf4j
public class TasksTrackerPluginPanel extends PluginPanel
{
	private final LoggedInPanel loggedInPanel;
	private final LoggedOutPanel loggedOutPanel = new LoggedOutPanel();

	public TaskListPanel taskListPanel;

	private boolean loggedIn = false;
	private TaskService taskService;

	public TasksTrackerPluginPanel(TasksTrackerPlugin plugin, TasksTrackerConfig config, SpriteManager spriteManager, TaskService taskService)
	{
		super(false);
		this.taskService = taskService;

		setBorder(new EmptyBorder(6, 6, 6, 6));
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		setLayout(new BorderLayout());

		loggedInPanel = new LoggedInPanel(plugin, config, taskService);
		taskListPanel = loggedInPanel.taskListPanel;
		add(loggedInPanel, BorderLayout.NORTH);
		loggedInPanel.setVisible(false);

		// Add error pane
		add(loggedOutPanel);
	}

	@Override
	public Dimension getPreferredSize()
	{
		return new Dimension(PANEL_WIDTH + SCROLLBAR_WIDTH, super.getPreferredSize().height);
	}

	public void redraw()
	{
		if (loggedIn)
		{
			loggedInPanel.redraw();
		}
	}

	public void refresh(TaskFromStruct task)
	{
		if (loggedIn)
		{
			loggedInPanel.refresh(task);
		}
	}

	public void refreshFilterButtonsFromConfig(ConfigValues.TaskListTabs tab)
	{
		if (loggedIn)
		{
			loggedInPanel.refreshFilterButtonsFromConfig(tab);
		}
	}

	public void setLoggedIn(boolean loggedIn)
	{
		if(SwingUtilities.isEventDispatchThread())
		{
            if (loggedIn != this.loggedIn)
            {
                if (loggedIn)
                {
                    loggedOutPanel.setVisible(false);
                    loggedInPanel.setVisible(true);
                }
                else
                {
                    loggedInPanel.setVisible(false);
                    loggedOutPanel.setVisible(true);
                }

                validate();
                repaint();
            }
            this.loggedIn = loggedIn;
		}
		else
		{
			log.error("Failed to update loggedIn state - not event dispatch thread.");
		}
	}

	public void drawNewTaskType()
	{
		if (loggedIn)
		{
			loggedInPanel.drawNewTaskType();
		}

	}
}