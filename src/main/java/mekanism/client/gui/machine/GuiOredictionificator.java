package mekanism.client.gui.machine;

import java.util.Collections;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.GuiElementHolder;
import mekanism.client.gui.element.button.FilterButton;
import mekanism.client.gui.element.button.TranslationButton;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.scroll.GuiScrollBar;
import mekanism.client.gui.element.window.filter.GuiOredictionificatorFilter;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.content.filter.FilterManager;
import mekanism.common.content.filter.IFilter;
import mekanism.common.content.oredictionificator.OredictionificatorItemFilter;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.inventory.warning.WarningTracker.WarningType;
import mekanism.common.network.to_server.PacketGuiInteract;
import mekanism.common.network.to_server.PacketGuiInteract.GuiInteraction;
import mekanism.common.tile.machine.TileEntityOredictionificator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GuiOredictionificator extends GuiConfigurableTile<TileEntityOredictionificator, MekanismTileContainer<TileEntityOredictionificator>> {

    /**
     * The number of filters that can be displayed
     */
    private static final int FILTER_COUNT = 3;

    private GuiScrollBar scrollBar;

    public GuiOredictionificator(MekanismTileContainer<TileEntityOredictionificator> container, Inventory inv, Component title) {
        super(container, inv, title);
        imageHeight += 64;
        inventoryLabelY = imageHeight - 94;
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        //Filter holder
        addRenderableWidget(new GuiElementHolder(this, 9, 17, 144, 68));
        //new filter button border
        addRenderableWidget(new GuiElementHolder(this, 9, 85, 144, 22));
        FilterManager<OredictionificatorItemFilter> filterManager = tile.getFilterManager();
        scrollBar = addRenderableWidget(new GuiScrollBar(this, 153, 17, 90, filterManager::count, () -> FILTER_COUNT));
        addRenderableWidget(new GuiProgress(() -> tile.didProcess, ProgressType.LARGE_RIGHT, this, 64, 119));
        addRenderableWidget(new TranslationButton(this, 10, 86, 142, 20, MekanismLang.BUTTON_NEW_FILTER,
              () -> addWindow(GuiOredictionificatorFilter.create(this, tile))));
        //Add each of the buttons and then just change visibility state to match filter info
        for (int i = 0; i < FILTER_COUNT; i++) {
            addRenderableWidget(new FilterButton(this, 10, 18 + i * 22, 142, 22, i, scrollBar::getCurrentSelection, filterManager, this::onClick,
                  index -> Mekanism.packetHandler().sendToServer(new PacketGuiInteract(GuiInteraction.TOGGLE_FILTER_STATE, tile, index)), filter -> {
                if (filter instanceof OredictionificatorItemFilter oredictionificatorFilter) {
                    return Collections.singletonList(oredictionificatorFilter.getResult());
                }
                return Collections.emptyList();
            })).warning(WarningType.INVALID_OREDICTIONIFICATOR_FILTER, filter -> filter != null && filter.isEnabled() && !filter.hasFilter());
        }
        //While we track and show warnings on the slots themselves we also need to track the warning
        // for if any of the set filters have it even if one of them is not visible
        // Note: We add this after all the buttons have their warnings added so that it is further down the tracker
        // so the tracker can short circuit on this type of warning and not have to check all the filters if one of
        // the ones that are currently being shown has the warning
        trackWarning(WarningType.INVALID_OREDICTIONIFICATOR_FILTER, () -> filterManager.anyEnabledMatch(filter -> !filter.hasFilter()));
    }

    protected void onClick(IFilter<?> filter, int index) {
        if (filter instanceof OredictionificatorItemFilter oredictionificatorFilter) {
            addWindow(GuiOredictionificatorFilter.edit(this, tile, oredictionificatorFilter));
        }
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        drawString(guiGraphics, playerInventoryTitle, inventoryLabelX, inventoryLabelY, titleTextColor());
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX, mouseY, delta) || scrollBar.adjustScroll(delta);
    }
}