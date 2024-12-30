package net.reldo.taskstracker;

import net.reldo.taskstracker.config.ConfigValues;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(TasksTrackerPlugin.CONFIG_GROUP_NAME)
public interface TasksTrackerConfig extends Config
{

    /*====================
    -- General settings --
    ====================*/

    @ConfigSection(
            name = "General",
            description = "General settings",
            position = 0
    )
    String generalSettings = "generalSettings";

	@ConfigItem(
		    position = 10,
		    keyName = "untrackUponCompletion",
		    name = "Untrack Tasks Upon Completion",
		    description = "Configures whether completed tasks should also automatically untracked when the task is completed.",
            section = generalSettings
	)
	default boolean untrackUponCompletion()
	{
		return false;
	}

    @ConfigItem(
            position = 11,
            keyName = "filterPanelCollapsible",
            name = "Filter Panels Collapsible",
            description = "Shows button that allows filter panels to be hidden.",
            section = generalSettings
    )
    default boolean filterPanelCollapsible()
    {
        return true;
    }

    @ConfigItem(
            position = 12,
            keyName = "saveSubFilterState", //@todo generalise this to all sub-filters
            name = "Save Filter State",
            description = "Configures whether the state of area filters should be saved and recalled when switching task type or restarting the plugin.",
            hidden = true //todo This is hidden because it currently doesn't do anything
    )
    default boolean saveSubFilterState()
    {
        return true;
    }


    @ConfigSection(
            name = "Internal Config",
            description = "These settings change the internal behaviour of the plugin. Reset them if any issues occur.",
            position = 10,
            closedByDefault = true
    )
    String internalConfig = "internalConfig";

    @Range(
            min = 10
    )
    @ConfigItem(
            position = 13,
            keyName = "taskPanelBatchSize",
            name = "Task Panel Batch Size",
            description = "Configures the number of task panels to create in each batch when redrawing the task list panel.",
            section = internalConfig
    )
    default int taskPanelBatchSize()
    {
        return 50;
    }

    @ConfigItem(
            position = 100,
            keyName = "completedFilter",
            name = "Completed Tasks Filter",
            description = "Configures whether completed tasks should be displayed.",
            hidden = true
    )
    default ConfigValues.CompletedFilterValues completedFilter()
    {
        return ConfigValues.CompletedFilterValues.COMPLETE_AND_INCOMPLETE;
    }

    @ConfigItem(
            position = 101,
            keyName = "trackedFilter",
            name = "Tracked Tasks Filter",
            description = "Configures whether tracked tasks should be displayed.",
            hidden = true
    )
    default ConfigValues.TrackedFilterValues trackedFilter()
    {
        return ConfigValues.TrackedFilterValues.TRACKED_AND_UNTRACKED;
    }

    @ConfigItem(
            position = 102,
            keyName = "ignoredFilter",
            name = "Ignored Tasks Filter",
            description = "Configures whether ignored tasks should be displayed.",
            hidden = true
    )
    default ConfigValues.IgnoredFilterValues ignoredFilter()
    {
        return ConfigValues.IgnoredFilterValues.NOT_IGNORED;
    }

    @ConfigItem(
            position = 103,
            keyName = "taskListTab",
            name = "Selected Task List Tab",
            description = "Configures the currently selected tab on the task list.",
            hidden = true
    )
    default ConfigValues.TaskListTabs taskListTab()
    {
        return ConfigValues.TaskListTabs.TAB_TWO;
    }

	@ConfigItem(
		    position = 106,
		    keyName = "taskTypeJsonName",
		    name = "Task Type",
		    description = "Configures the task type which is displayed in the panel.",
		    hidden = true
	)
	default String taskTypeJsonName()
	{
		return "COMBAT";
	}

	@ConfigItem(
		    position = 109,
		    keyName = "dropdownFilter",
		    name = "Dropdown Filter",
		    description = "Configures the dropdown to filter tasks on.",
		    hidden = true
	)
	default String dropdownFilter()
	{
		return "";
	}

    @ConfigItem(
            position = 110,
            keyName = "sortCriteria",
            name = "Sort Criteria",
            description = "Configures the criteria to sort tasks on.",
            hidden = true
    )
    default String sortCriteria()
    {
        return "Default";
    }

    @ConfigItem(
            position = 111,
            keyName = "sortDirection",
            name = "Sort Direction",
            description = "Configures the direction to sort tasks.",
            hidden = true
    )
    default ConfigValues.SortDirections sortDirection()
    {
        return ConfigValues.SortDirections.ASCENDING;
    }

    /*==========================
    -- Task List Tab settings --
    ==========================*/

    /*==================
    -- Tab 1 settings --
    ==================*/

    @ConfigSection(
            name = "Task List Tab 1",
            description = "Tab 1 settings",
            position = 1
    )
    String tab1Settings = "tab1Settings";

    @ConfigItem(
            position = 1,
            keyName = "tab1Name",
            name = "Name",
            description = "The name of the tab. Default: Tracked Tasks",
            section = tab1Settings
    )
    default String tab1Name()
    {
        return "Tracked";
    }

    @ConfigItem(
            position = 2,
            keyName = "tab1CompletedLock",
            name = "Lock Completed Filter",
            description = "Locks the completed tasks filter button to the configured value when tab 1 is selected.",
            section = tab1Settings
    )
    default boolean tab1CompletedLock()
    {
        return false;
    }

    @ConfigItem(
            position = 3,
            keyName = "tab1CompletedValue",
            name = "Completed Filter",
            description = "The configured completed tasks filter button value.",
            section = tab1Settings
    )
    default ConfigValues.CompletedFilterValues tab1CompletedValue()
    {
        return ConfigValues.CompletedFilterValues.INCOMPLETE;
    }

    @ConfigItem(
            position = 4,
            keyName = "tab1TrackedLock",
            name = "Lock Tracked Filter",
            description = "Locks the tracked tasks filter button to the configured value when tab 1 is selected.",
            section = tab1Settings
    )
    default boolean tab1TrackedLock()
    {
        return false;
    }

    @ConfigItem(
            position = 5,
            keyName = "tab1TrackedValue",
            name = "Tracked Filter",
            description = "The configured tracked tasks filter button value.",
            section = tab1Settings
    )
    default ConfigValues.TrackedFilterValues tab1TrackedValue()
    {
        return ConfigValues.TrackedFilterValues.TRACKED;
    }

    @ConfigItem(
            position = 6,
            keyName = "tab1IgnoredLock",
            name = "Lock Ignored Filter",
            description = "Locks the ignored tasks filter button to the configured value when tab 1 is selected.",
            section = tab1Settings
    )
    default boolean tab1IgnoredLock()
    {
        return false;
    }

    @ConfigItem(
            position = 7,
            keyName = "tab1IgnoredValue",
            name = "Ignored Filter",
            description = "The configured ignored tasks filter button value.",
            section = tab1Settings
    )
    default ConfigValues.IgnoredFilterValues tab1IgnoredValue()
    {
        return ConfigValues.IgnoredFilterValues.NOT_IGNORED;
    }

    /*==================
    -- Tab 2 settings --
    ==================*/

    @ConfigSection(
            name = "Task List Tab 2",
            description = "Tab 2 settings",
            position = 2
    )
    String tab2Settings = "tab2Settings";

    @ConfigItem(
            position = 1,
            keyName = "tab2Name",
            name = "Name",
            description = "The name of the tab. Default: All Tasks",
            section = tab2Settings
    )
    default String tab2Name()
    {
        return "Incomplete";
    }

    @ConfigItem(
            position = 2,
            keyName = "tab2CompletedLock",
            name = "Lock Completed Filter",
            description = "Locks the completed tasks filter button to the configured value when tab 2 is selected.",
            section = tab2Settings
    )
    default boolean tab2CompletedLock()
    {
        return false;
    }

    @ConfigItem(
            position = 3,
            keyName = "tab2CompletedValue",
            name = "Completed Filter",
            description = "The configured completed tasks filter button value.",
            section = tab2Settings
    )
    default ConfigValues.CompletedFilterValues tab2CompletedValue()
    {
        return ConfigValues.CompletedFilterValues.INCOMPLETE;
    }

    @ConfigItem(
            position = 4,
            keyName = "tab2TrackedLock",
            name = "Lock Tracked Filter",
            description = "Locks the tracked tasks filter button to the configured value when tab 2 is selected.",
            section = tab2Settings
    )
    default boolean tab2TrackedLock()
    {
        return false;
    }

    @ConfigItem(
            position = 5,
            keyName = "tab2TrackedValue",
            name = "Tracked Filter",
            description = "The configured tracked tasks filter button value.",
            section = tab2Settings
    )
    default ConfigValues.TrackedFilterValues tab2TrackedValue()
    {
        return ConfigValues.TrackedFilterValues.TRACKED_AND_UNTRACKED;
    }

    @ConfigItem(
            position = 6,
            keyName = "tab2IgnoredLock",
            name = "Lock Ignored Filter",
            description = "Locks the ignored tasks filter button to the configured value when tab 2 is selected.",
            section = tab2Settings
    )
    default boolean tab2IgnoredLock()
    {
        return false;
    }

    @ConfigItem(
            position = 7,
            keyName = "tab2IgnoredValue",
            name = "Ignored Filter",
            description = "The configured ignored tasks filter button value.",
            section = tab2Settings
    )
    default ConfigValues.IgnoredFilterValues tab2IgnoredValue()
    {
        return ConfigValues.IgnoredFilterValues.NOT_IGNORED;
    }

    /*==================
    -- Tab 3 settings --
    ==================*/

    @ConfigSection(
            name = "Task List Tab 3",
            description = "Tab 3 settings",
            position = 3
    )
    String tab3Settings = "tab3Settings";

    @ConfigItem(
            position = 1,
            keyName = "tab3Name",
            name = "Name",
            description = "The name of the tab. Default: Custom",
            section = tab3Settings
    )
    default String tab3Name()
    {
        return "All Tasks";
    }

    @ConfigItem(
            position = 2,
            keyName = "tab3CompletedLock",
            name = "Lock Completed Filter",
            description = "Locks the completed tasks filter button to the configured value when tab 3 is selected.",
            section = tab3Settings
    )
    default boolean tab3CompletedLock()
    {
        return false;
    }

    @ConfigItem(
            position = 3,
            keyName = "tab3CompletedValue",
            name = "Completed Filter",
            description = "The configured completed tasks filter button value.",
            section = tab3Settings
    )
    default ConfigValues.CompletedFilterValues tab3CompletedValue()
    {
        return ConfigValues.CompletedFilterValues.COMPLETE_AND_INCOMPLETE;
    }

    @ConfigItem(
            position = 4,
            keyName = "tab3TrackedLock",
            name = "Lock Tracked Filter",
            description = "Locks the tracked tasks filter button to the configured value when tab 3 is selected.",
            section = tab3Settings
    )
    default boolean tab3TrackedLock()
    {
        return false;
    }

    @ConfigItem(
            position = 5,
            keyName = "tab3TrackedValue",
            name = "Tracked Filter",
            description = "The configured tracked tasks filter button value.",
            section = tab3Settings
    )
    default ConfigValues.TrackedFilterValues tab3TrackedValue()
    {
        return ConfigValues.TrackedFilterValues.TRACKED_AND_UNTRACKED;
    }

    @ConfigItem(
            position = 6,
            keyName = "tab3IgnoredLock",
            name = "Lock Ignored Filter",
            description = "Locks the ignored tasks filter button to the configured value when tab 3 is selected.",
            section = tab3Settings
    )
    default boolean tab3IgnoredLock()
    {
        return false;
    }

    @ConfigItem(
            position = 7,
            keyName = "tab3IgnoredValue",
            name = "Ignored Filter",
            description = "The configured ignored tasks filter button value.",
            section = tab3Settings
    )
    default ConfigValues.IgnoredFilterValues tab3IgnoredValue()
    {
        return ConfigValues.IgnoredFilterValues.IGNORED_AND_NOT_IGNORED;
    }


}
