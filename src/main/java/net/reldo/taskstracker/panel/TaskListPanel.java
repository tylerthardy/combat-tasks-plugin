package net.reldo.taskstracker.panel;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.reldo.taskstracker.TasksTrackerPlugin;
import net.reldo.taskstracker.config.ConfigValues;
import net.reldo.taskstracker.data.jsondatastore.types.TaskDefinitionSkill;
import net.reldo.taskstracker.data.task.TaskFromStruct;
import net.reldo.taskstracker.data.task.TaskService;
import net.reldo.taskstracker.panel.components.FixedWidthPanel;
import net.runelite.api.Skill;
import net.runelite.client.ui.FontManager;

@Slf4j
public class TaskListPanel extends JScrollPane
{
	public TasksTrackerPlugin plugin;
	private final int TASK_LIST_BUFFER_COUNT = 2;
	public ArrayList<TaskPanel> taskPanels = new ArrayList<>();
	private final ArrayList<TaskListListPanel> taskListBuffers = new ArrayList<>(TASK_LIST_BUFFER_COUNT);
	private int currentTaskListBufferIndex;
	private final TaskService taskService;
	private final JLabel emptyTasks = new JLabel();
	@Setter
    private int batchSize;

	public TaskListPanel(TasksTrackerPlugin plugin, TaskService taskService)
	{
		this.plugin = plugin;
		this.taskService = taskService;
		batchSize = plugin.getConfig().taskPanelBatchSize();

		FixedWidthPanel taskListListPanelWrapper = new FixedWidthPanel();

		for(int i = 0; i < TASK_LIST_BUFFER_COUNT; i++)
		{
			taskListBuffers.add(new TaskListListPanel(plugin));
			taskListListPanelWrapper.add(taskListBuffers.get(i));
		}

		setViewportView(taskListListPanelWrapper);
		setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

		setCurrentTaskListListPanel(0);
	}

	private void setCurrentTaskListListPanel(int index)
	{
		if (index != currentTaskListBufferIndex)
		{
			taskListBuffers.get(currentTaskListBufferIndex).setVisible(false);
			taskListBuffers.get(index).setVisible(true);
			currentTaskListBufferIndex = index;
		}
	}

	private TaskListListPanel getCurrentTaskListListPanel()
	{
		return taskListBuffers.get(currentTaskListBufferIndex);
	}

	private TaskListListPanel getNextTaskListListPanel()
	{
		return taskListBuffers.get(getNextBufferIndex());
	}

	private void showNextTaskListListPanel()
	{
		log.debug("Showing next task list list panel: {}", getNextBufferIndex());
		TaskListListPanel previousPanel = getCurrentTaskListListPanel();
		setCurrentTaskListListPanel(getNextBufferIndex());
		previousPanel.prepEmptyTaskListPanel();
	}

	private int getNextBufferIndex()
	{
		return (currentTaskListBufferIndex + 1) % TASK_LIST_BUFFER_COUNT;
	}

	public void drawNewTaskType()
	{
		log.debug("Drawing new Task Type taskListListPanel");
		getNextTaskListListPanel().drawNewTaskType();
	}

	public void redraw()
	{
		log.debug("Redrawing taskListListPanel");
		getCurrentTaskListListPanel().redraw();
	}

	public void refresh(TaskFromStruct task)
	{
		if(SwingUtilities.isEventDispatchThread())
		{
			emptyTasks.setVisible(false);

			if (task != null)
			{
				Optional<TaskPanel> panel = taskPanels.stream()
					.filter(tp -> tp.task.getName().equalsIgnoreCase(task.getName()))
					.findFirst();
				panel.ifPresent(TaskPanel::refresh);
			}
			else
			{
				for (TaskPanel taskPanel : taskPanels)
				{
					taskPanel.refresh();
				}
			}

			refreshEmptyPanel();
		}
		else
		{
			log.error("Task list panel refresh failed - not event dispatch thread.");
		}
	}

	private void refreshEmptyPanel()
	{
		Optional<TaskPanel> visibleTaskPanel = taskPanels.stream()
				.filter(TaskPanel::isVisible)
				.findFirst();

		if (visibleTaskPanel.isEmpty())
		{
			emptyTasks.setVisible(true);
		}
	}

	public void refreshTaskPanelsWithSkill(Skill skill)
	{
		// Refresh all task panels for tasks with 'skill' or
		// 'SKILLS' (any skill) or 'TOTAL LEVEL' as a requirement.
		taskPanels.stream()
			.filter(tp ->
			{
				List<TaskDefinitionSkill> skillsList = tp.task.getTaskDefinition().getSkills();
				if(skillsList == null || skillsList.isEmpty())
				{
					return false;
				}

				return skillsList.stream()
					.map(TaskDefinitionSkill::getSkill)
					.anyMatch(s ->  s.equalsIgnoreCase(skill.getName()) ||
									s.equalsIgnoreCase("SKILLS") ||
									s.equalsIgnoreCase("TOTAL LEVEL")
					);
			})
			.forEach(TaskPanel::refresh);
	}

	public String getEmptyTaskListMessage()
	{
		return "No tasks match the current filters.";
	}

    private class TaskListListPanel extends FixedWidthPanel
	{
		private final TasksTrackerPlugin plugin;

		public TaskListListPanel(TasksTrackerPlugin plugin)
		{
			this.plugin = plugin;

			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			setBorder(new EmptyBorder(0, 10, 10, 10));
			setAlignmentX(Component.LEFT_ALIGNMENT);

			emptyTasks.setBorder(new EmptyBorder(10,0,10,0));
			emptyTasks.setText("<html><center>" + getEmptyTaskListMessage() + "</center></html>");
			emptyTasks.setFont(FontManager.getRunescapeSmallFont());
			emptyTasks.setHorizontalAlignment(JLabel.CENTER);
			emptyTasks.setVerticalAlignment(JLabel.CENTER);
			add(emptyTasks);
			emptyTasks.setVisible(false);
		}

		public void prepEmptyTaskListPanel()
		{
			SwingUtilities.invokeLater(this::removeAll);
		}

		public void drawNewTaskType()
		{
			log.debug("TaskListPanel.drawNewTaskType");
			if(SwingUtilities.isEventDispatchThread())
			{
				log.debug("TaskListPanel creating panels");

				add(emptyTasks);

				List<TaskFromStruct> tasks = taskService.getTasks();
				if (tasks == null || tasks.isEmpty())
				{
					emptyTasks.setVisible(true);
					return;
				}

				emptyTasks.setVisible(false);

				// Buffer to hold newly created task panels before they are swapped in
				ArrayList<TaskPanel> newTaskPanels = new ArrayList<>(tasks.size());

				processInBatches(tasks.size(), indexPosition ->
				{
					TaskPanel taskPanel = new TaskPanel(plugin, tasks.get(indexPosition));
					add(taskPanel);
					newTaskPanels.add(taskPanel);
					if (indexPosition == (batchSize - 1)) taskPanels = newTaskPanels; // replace taskPanels list at end of first batch
				});
			}
			else
			{
				log.error("Task list panel drawNewTaskType failed - not event dispatch thread.");
			}
		}

		public void redraw()
		{
			log.debug("TaskListPanel.redraw");
			if(SwingUtilities.isEventDispatchThread())
			{
				if (taskPanels == null || taskPanels.isEmpty())
				{
					emptyTasks.setVisible(true);
					return;
				}

				for (int indexPosition = 0; indexPosition < taskPanels.size(); indexPosition++)
				{
					int adjustedIndexPosition = indexPosition;
					if (plugin.getConfig().sortDirection().equals(ConfigValues.SortDirections.DESCENDING))
						adjustedIndexPosition = taskPanels.size() - (adjustedIndexPosition + 1);
					TaskPanel taskPanel = taskPanels.get(taskService.getSortedTaskIndex(plugin.getConfig().sortCriteria(), adjustedIndexPosition));
					setComponentZOrder(taskPanel, indexPosition);
				}

				SwingUtilities.invokeLater(() -> refresh(null));
			}
			else
			{
				log.error("Task list panel redraw failed - not event dispatch thread.");
			}
		}

		private void processInBatches(int objectCount, IntConsumer method)
		{
			processBatch(0, objectCount, method);
			showNextTaskListListPanel();
		}

		private void processBatch(int batch, int objectCount, IntConsumer method)
		{
			log.debug("TaskListPanel.processBatch {}", batch);

			for (int index = 0; index < batchSize; index++)
			{
				int indexPosition = index + (batch * batchSize);
				if (indexPosition < objectCount)
				{
					method.accept(indexPosition);
				}
				else
				{
					break;
				}
			}

			refreshEmptyPanel();
			validate();
			repaint();

			// queue next batch if not done or refresh after last batch
			int batchIndex = batch + 1;
			if (batchIndex * batchSize < objectCount)
			{
				SwingUtilities.invokeLater(() -> processBatch(batchIndex, objectCount, method));
			}
			else
			{
				SwingUtilities.invokeLater(() -> refresh(null));
			}
		}
	}
}
