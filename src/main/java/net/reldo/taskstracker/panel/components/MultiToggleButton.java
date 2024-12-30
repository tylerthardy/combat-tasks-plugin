package net.reldo.taskstracker.panel.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;
import lombok.Getter;
import lombok.Setter;

public class MultiToggleButton extends JButton
{

    // Icons
    private final Icon[] icons;
    // Tooltips
    private final String[] tooltips;
    private final int stateCount;
    @Setter
    private ActionListener stateChangedAction = null;
    @Getter
    private int state = 0;
    final JPopupMenu popupMenu = new JPopupMenu();
    private boolean popupMenuEnabled = false;

    public MultiToggleButton(int stateCount)
    {
        super();
        this.stateCount = stateCount;
        icons = new Icon[stateCount];
        tooltips = new String[stateCount];
        popupMenu.setBorder(new EmptyBorder(5, 5, 5, 5));
        addActionListener(e -> changeStateThenAction());
    }

    public void popupMenuEnabled(boolean enabled)
    {
        if(popupMenuEnabled != enabled)
        {
            popupMenuEnabled = enabled;
            if(popupMenuEnabled)
            {
                this.setComponentPopupMenu(popupMenu);
            }
            else
            {
                this.setComponentPopupMenu(null);
            }
        }
    }

    public void setIcon(Icon icon, int state)
    {
        if (state < 0 || state > stateCount || icon == null)
        {
            return;
        }

        icons[state] = icon;

        if (state == this.state) setIconState();
    }

    public boolean setIcons(Icon[] icons)
    {
        if (icons == null || icons.length == 0)
        {
            return false;
        }

        for (int i = 0; i < icons.length; i++)
        {
            setIcon(icons[i], i);
        }

        return true;
    }

    public void setToolTip(String tooltip, int state)
    {
        if (state < 0 || state > 3 || tooltip == null)
        {
            return;
        }

        tooltips[state] = tooltip;
        addPopupMenuItem(tooltip, state);

        if (state == this.state) setTooltipState();
    }

    public boolean setToolTips(String[] tooltips)
    {
        if (tooltips == null || tooltips.length == 0)
        {
            return false;
        }

        for (int i = 0; i < tooltips.length; i++)
        {
            setToolTip(tooltips[i], i);
        }

        return true;
    }

    public void changeState()
    {
        setState((++state) % stateCount);
    }

    public void changeStateThenAction()
    {
        setStateThenAction((++state) % stateCount);
    }

    private void setIconState()
    {
        super.setIcon(icons[state]);
    }

    private void setTooltipState()
    {
        super.setToolTipText(tooltips[state]);
    }

    public void setState(int state)
    {
        this.state = state;
        setIconState();
        setTooltipState();
    }

    public void setStateThenAction(int state)
    {
        setState(state);
        if(stateChangedAction != null) this.stateChangedAction.actionPerformed(new ActionEvent(this, 0, ""));
    }

    private void addPopupMenuItem(String text, int state)
    {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(e -> {if(isEnabled())setStateThenAction(state);});
        popupMenu.add(menuItem);
    }

    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        popupMenuEnabled(enabled);
    }
}
