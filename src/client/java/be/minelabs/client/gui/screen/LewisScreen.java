package be.minelabs.client.gui.screen;


import be.minelabs.Minelabs;
import be.minelabs.crafting.lewis.LewisCraftingGrid;
import be.minelabs.crafting.molecules.BondManager;
import be.minelabs.crafting.molecules.MoleculeItemGraph;
import be.minelabs.crafting.molecules.ValenceElectrons;
import be.minelabs.screen.LewisBlockScreenHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.SpriteIdentifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class LewisScreen extends HandledScreen<LewisBlockScreenHandler> implements ScreenHandlerProvider<LewisBlockScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(Minelabs.MOD_ID, "textures/gui/lewis_block/lewis_block_inventory_craftable.png");
    private static final Identifier TEXTURE2 = new Identifier(Minelabs.MOD_ID, "textures/gui/lewis_block/lewis_block_inventory.png");
    private ButtonWidget buttonWidget;
    private boolean widgetTooltip = false;

    public LewisScreen(LewisBlockScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        // 3x18 for 3 inventory slots | +4 for extra offset to match the double chest | +5 for the row between the 5x5 grid and the input slots
        backgroundHeight += (18 * 3 + 4) + 5;

        // Add button to clear input/grid
        registerButtonWidget();
    }

    public ButtonWidget getButtonWidget() {
        return buttonWidget;
    }

    /*
     * draw function is called every tick
     */
    @Override
    protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (this.handler.hasRecipe()) {
            RenderSystem.setShaderTexture(0, TEXTURE);
        } else {
            RenderSystem.setShaderTexture(0, TEXTURE2);
        }

        drawTexture(matrices, x, y, 0, 0, backgroundWidth, backgroundHeight);
        renderProgressArrow(matrices, this.x, this.y);
        renderRecipeCheck(matrices, this.x, this.y);
        buttonWidget.renderButton(matrices, mouseX, mouseY, delta);

        // Keep mapping between stack (in graph) and slots (for rendering)
        Map<ItemStack, Slot> stackToSlotMap = new HashMap<>();
        for (int i = 0; i < LewisBlockScreenHandler.GRIDSIZE; i++) {
            stackToSlotMap.put(handler.getLewisCraftingGrid().getStack(i), handler.getSlot(i));
        }

        /*
         * Draw Bonds on screen
         */
        LewisCraftingGrid grid = handler.getLewisCraftingGrid();
        BondManager manager = new BondManager();
        if (grid.getPartialMolecule().getStructure() instanceof MoleculeItemGraph graph){
            for (MoleculeItemGraph.Edge edge : graph.getEdges()) {
                Slot slot1 = stackToSlotMap.get(graph.getItemStackOfVertex(edge.getFirst()));
                Slot slot2 = stackToSlotMap.get(graph.getItemStackOfVertex(edge.getSecond()));
                BondManager.Bond bond = manager.getOrCreateBond(slot1, slot2, edge.data.bondOrder);
                this.itemRenderer.renderInGuiWithOverrides(matrices, bond.getStack(), bond.getX() + x, bond.getY() + y);
            }
            for (MoleculeItemGraph.Vertex vertex : graph.getVertices()) {
                Slot slot = stackToSlotMap.get(graph.getItemStackOfVertex(vertex));
                int total_bonds = 0;
                Map<String, Integer> bonds = manager.findEmptyBonds(slot);
                for (String key : bonds.keySet()) {
                    total_bonds += bonds.get(key);
                }

                ValenceElectrons valentieE = new ValenceElectrons(bonds,
                        vertex.data.getInitialValenceElectrons() - vertex.getEdgesData().stream().map(bond -> bond.bondOrder).mapToInt(Integer::intValue).sum()
                        , vertex.data.getMaxPossibleBonds() == total_bonds); //no clue: copied it from getOpenConnections in the MoleculeGraph class
                for (String i : Arrays.asList("n", "e", "s", "w")) { //render item 4x: N-E-S-W
                    if (valentieE.getDirectionalValence(i) != 0) {
                        this.itemRenderer.renderInGuiWithOverrides(matrices, valentieE.getStack(i), slot.x + x, slot.y + y);
                    }
                }
            }
        }

        /*
         * Render input slot overlays
         */
        DefaultedList<Ingredient> ingredients = handler.getIngredients();
        for (int i = 0; i < ingredients.size(); i++) {

            ItemStack atom = ingredients.get(i).getMatchingStacks()[0];
            if (!this.handler.hasRecipe() || atom.isEmpty()) {
                break;
            }
            String atom_item_name = atom.getItem().toString();
            SpriteIdentifier SPRITE_ID = new SpriteIdentifier(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE, new Identifier(Minelabs.MOD_ID, "item/"+StringUtils.removeEnd(atom_item_name,"_atom")));

            RenderSystem.setShaderColor(0.2F, 0.2F, 0.2F, 0.8F);
            RenderSystem.enableBlend();
            matrices.push();
            matrices.scale(0.5f, 0.5f, 0.5f);
            drawSprite(matrices, 2*(x + 8 + 18*i), 2*(133+y-20), 1, 32, 32, SPRITE_ID.getSprite());
            //MinecraftClient.getInstance().textRenderer.draw(matrices, Integer.toString(handler.getDensity()), 2*(x + 8 + 18*i)+24, (int) 2*(133+y-20)+24, 5592405);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
            matrices.pop();

            if (handler.getIoInventory().getStack(i).getCount() < handler.getDensity()) {
                //this.itemRenderer.renderInGuiWithOverrides(new ItemStack(Items.RED_STAINED_GLASS_PANE), x + 8 + 18 * i, 133 + y - 20);
            } else {
                this.itemRenderer.renderInGuiWithOverrides(matrices, new ItemStack(Items.GREEN_STAINED_GLASS_PANE), x + 8 + 18 * i, 133 + y - 20);
            }
            //this.itemRenderer.renderInGuiWithOverrides(atom, x + 8 + 18*i, 133+y-20);
            RenderSystem.disableBlend();
        }
        matrices.push();
        matrices.scale(0.5f, 0.5f, 0.5f);
        for (int i = 0; i < ingredients.size(); i++) { //yes, separate loop... the drawtext somehow changes things to the rendering and does not reset after
            ItemStack atom = ingredients.get(i).getMatchingStacks()[0];
            if (!this.handler.hasRecipe() || atom.isEmpty()) {
                break;
            }
            if (handler.getIoInventory().getStack(i).getCount() == 0) {
                MinecraftClient.getInstance().textRenderer.draw(matrices, Integer.toString(handler.getDensity()), 2 * (x + 8 + 18 * i) + 25, (int) 2 * (133 + y - 20) + 23, 5592405);
            }
        }
        matrices.pop();
    }


    @Override
    protected void init() {
        super.init();

        // move the title to the correct place
        playerInventoryTitleY += 61;

        registerButtonWidget();
    }


    @SuppressWarnings("ConstantConditions")
    private void registerButtonWidget() {
        buttonWidget = new ButtonWidget.Builder(Text.of("C"), button -> {
            if (!widgetTooltip) return;
            if (handler.isInputEmpty()) {
                for (int i = 0; i < LewisBlockScreenHandler.GRIDSIZE; i++) {
                    client.interactionManager.clickSlot(handler.syncId, i, 0, SlotActionType.PICKUP, client.player);
                }
            } else {
                for (int i = 0; i < 9; i++) {
                    client.interactionManager.clickSlot(handler.syncId, i + LewisBlockScreenHandler.GRIDSIZE, 0, SlotActionType.QUICK_MOVE, client.player);
                }
            }
        }).position(x+133, y+17).size(18,18)
          /*.tooltip(Tooltip.of(handler.isInputEmpty() ?
                  Text.translatableWithFallback("text.minelabs.clear_grid", "Clear Grid") :
                  Text.translatableWithFallback("text.minelabs.clear_slots", "Clear Slots")))*/
          .build();
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);

        widgetTooltip = false;
        if (this.handler.getCursorStack().isEmpty()) {
            if (this.focusedSlot != null && this.focusedSlot.hasStack())
                this.renderTooltip(matrices, this.focusedSlot.getStack(), mouseX, mouseY);
            if (buttonWidget.isMouseOver(mouseX, mouseY)){
                renderTooltip(matrices, handler.isInputEmpty() ?
                        Text.translatableWithFallback("text.minelabs.clear_grid", "Clear Grid") :
                        Text.translatableWithFallback("text.minelabs.clear_slots", "Clear Slots"), mouseX, mouseY);
                widgetTooltip = true;
            }
            if (mouseX >= x + 105 && mouseX < x + 105 + 16 && mouseY >= y + 17 && mouseY < y + 17 + 16) {
                switch (handler.getStatus()) {
                    case 1 -> renderTooltip(matrices, Arrays.asList(
                            Text.translatable("text.minelabs.valid"),
                            Text.translatable("text.minelabs.not_implemented")), mouseX, mouseY);
                    case 2 -> renderTooltip(matrices,
                            Text.translatable("text.minelabs.valid"), mouseX, mouseY);
                    case 3 ->  renderTooltip(matrices,
                            Text.translatable("text.minelabs.multiple_molecules"), mouseX, mouseY);
                    default -> renderTooltip(matrices,
                            Text.translatable("text.minelabs.invalid"), mouseX, mouseY);
                }

            }
        }
    }

    private void renderProgressArrow(MatrixStack matrices, int x, int y) {
        if (handler.isCrafting()) {
            drawTexture(matrices, x + 102, y + 52, 176, 0, handler.getScaledProgress(), 20);
        }
    }

    private void renderRecipeCheck(MatrixStack matrices, int x, int y) {
        switch (handler.getStatus()) {
            case 1 -> drawTexture(matrices, x + 105, y + 17, 176, 38, 16, 16); // NOT IMPLEMENTED
            case 2 -> drawTexture(matrices, x + 105, y + 17, 176, 55, 16, 16); // VALID
            case 3 -> drawTexture(matrices, x + 105, y + 17, 176, 38, 16, 16);
            default -> drawTexture(matrices, x + 105, y + 17, 176, 21, 16, 16); // INVALID
        }
    }
}
