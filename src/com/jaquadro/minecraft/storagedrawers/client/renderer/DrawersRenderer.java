package com.jaquadro.minecraft.storagedrawers.client.renderer;

import com.jaquadro.minecraft.storagedrawers.StorageDrawers;
import com.jaquadro.minecraft.storagedrawers.api.storage.IDrawer;
import com.jaquadro.minecraft.storagedrawers.block.BlockCompDrawers;
import com.jaquadro.minecraft.storagedrawers.block.BlockDrawers;
import com.jaquadro.minecraft.storagedrawers.block.tile.TileEntityDrawers;
import com.jaquadro.minecraft.storagedrawers.util.RenderHelper;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;
import org.lwjgl.opengl.GL11;

public class DrawersRenderer implements ISimpleBlockRenderingHandler
{
    private static final double unit = .0625f;

    private ModularBoxRenderer boxRenderer = new ModularBoxRenderer();

    private double[] boxCoord = new double[6];

    @Override
    public void renderInventoryBlock (Block block, int metadata, int modelId, RenderBlocks renderer) {
        if (!(block instanceof BlockDrawers))
            return;

        renderInventoryBlock((BlockDrawers) block, metadata, modelId, renderer);
    }

    private void renderInventoryBlock (BlockDrawers block, int metadata, int modelId, RenderBlocks renderer) {
        int side = 4;

        boxRenderer.setUnit(unit);
        boxRenderer.setColor(ModularBoxRenderer.COLOR_WHITE);
        for (int i = 0; i < 6; i++)
            boxRenderer.setIcon(block.getIcon(i, metadata), i);

        GL11.glRotatef(90, 0, 1, 0);
        GL11.glTranslatef(-.5f, -.5f, -.5f);

        renderExterior(block, 0, 0, 0,side, renderer);

        boxRenderer.setUnit(0);
        boxRenderer.setInteriorIcon(block.getIcon(side, metadata), ForgeDirection.OPPOSITES[side]);

        renderInterior(block, 0, 0, 0, side, renderer);

        GL11.glTranslatef(.5f, .5f, .5f);
    }

    @Override
    public boolean renderWorldBlock (IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        if (!(block instanceof BlockDrawers))
            return false;

        return renderWorldBlock(world, x, y, z, (BlockDrawers) block, modelId, renderer);
    }

    private boolean renderWorldBlock (IBlockAccess world, int x, int y, int z, BlockDrawers block, int modelId, RenderBlocks renderer) {
        TileEntityDrawers tile = block.getTileEntity(world, x, y, z);
        if (tile == null)
            return false;

        int side = tile.getDirection();
        int meta = world.getBlockMetadata(x, y, z);

        boxRenderer.setUnit(block.trimWidth);
        boxRenderer.setColor(ModularBoxRenderer.COLOR_WHITE);
        for (int i = 0; i < 6; i++)
            boxRenderer.setExteriorIcon(block.getIcon(world, x, y, z, i), i);

        boxRenderer.setCutIcon(block.getIconTrim(meta));
        boxRenderer.setInteriorIcon(block.getIconTrim(meta));

        renderExterior(block, x, y, z, side, renderer);

        if (tile.getStorageLevel() > 1 && StorageDrawers.config.cache.renderStorageUpgrades) {
            for (int i = 0; i < 6; i++)
                boxRenderer.setExteriorIcon(block.getOverlayIcon(world, x, y, z, i, tile.getStorageLevel()), i);

            boxRenderer.setCutIcon(block.getOverlayIconTrim(tile.getStorageLevel()));
            boxRenderer.setInteriorIcon(block.getOverlayIconTrim(tile.getStorageLevel()));

            renderExterior(block, x, y, z, side, renderer);
        }

        boxRenderer.setUnit(0);
        boxRenderer.setInteriorIcon(block.getIcon(world, x, y, z, side), ForgeDirection.OPPOSITES[side]);

        renderInterior(block, x, y, z, side, renderer);

        if (StorageDrawers.config.cache.enableIndicatorUpgrades)
            renderIndicator(block, x, y, z, side, renderer, tile.getStatusLevel());

        return true;
    }

    private static final int[] cut = new int[] {
        ModularBoxRenderer.CUT_YPOS | ModularBoxRenderer.CUT_YNEG | ModularBoxRenderer.CUT_XPOS | ModularBoxRenderer.CUT_XNEG | ModularBoxRenderer.CUT_ZPOS,
        ModularBoxRenderer.CUT_YPOS | ModularBoxRenderer.CUT_YNEG | ModularBoxRenderer.CUT_XPOS | ModularBoxRenderer.CUT_XNEG | ModularBoxRenderer.CUT_ZNEG,
        ModularBoxRenderer.CUT_YPOS | ModularBoxRenderer.CUT_YNEG | ModularBoxRenderer.CUT_XPOS | ModularBoxRenderer.CUT_ZNEG | ModularBoxRenderer.CUT_ZPOS,
        ModularBoxRenderer.CUT_YPOS | ModularBoxRenderer.CUT_YNEG | ModularBoxRenderer.CUT_XNEG | ModularBoxRenderer.CUT_ZNEG | ModularBoxRenderer.CUT_ZPOS,
    };

    private static final float[][] drawerXYWH1 = new float[][] {
        { 0, 0, 16, 16 },
    };

    private static final float[][] drawerXYWH2 = new float[][] {
        { 0, 8, 16, 8 }, { 0, 0, 16, 8 },
    };

    private static final float[][] drawerXYWH4 = new float[][] {
        { 0, 8, 8, 8 }, { 0, 0, 8, 8 }, { 8, 8, 8, 8 }, { 8, 0, 8, 8 },
    };

    private void renderIndicator (BlockDrawers block, int x, int y, int z, int side, RenderBlocks renderer, int level) {
        if (level <= 0 || side < 2 || side > 5)
            return;

        TileEntityDrawers tile = block.getTileEntity(renderer.blockAccess, x, y, z);

        double depth = block.halfDepth ? 8 : 16;
        double depthAdj = block.trimDepth * 16;

        int count = 0;
        float[][] xywhSet = null;
        if (block.drawerCount == 1) {
            count = 1;
            xywhSet = drawerXYWH1;
        }
        else if (block.drawerCount == 2) {
            count = 2;
            xywhSet = drawerXYWH2;
        }
        else if (block.drawerCount == 4) {
            count = 4;
            xywhSet = drawerXYWH4;
        }

        IIcon iconOff = block.getIndicatorIcon(count, false);
        IIcon iconOn = block.getIndicatorIcon(count, true);

        for (int i = 0; i < count; i++) {
            IDrawer drawer = tile.getDrawer(i);
            if (drawer == null)
                continue;

            float[] xywh = xywhSet[i];

            setCoord(boxCoord, xywh[0] * unit, xywh[1] * unit, (depth - depthAdj) * unit, (xywh[0] + xywh[2]) * unit, (xywh[1] + xywh[3]) * unit, (depth - depthAdj + .05) * unit, side);

            boxRenderer.setExteriorIcon(iconOff);
            boxRenderer.renderExterior(renderer, block, x, y, z, boxCoord[0], boxCoord[1], boxCoord[2], boxCoord[3], boxCoord[4], boxCoord[5], 0, cut[side - 2]);

            if (level == 1 && drawer.getMaxCapacity() > 0 && drawer.getRemainingCapacity() == 0) {
                setCoord(boxCoord, xywh[0] * unit, xywh[1] * unit, (depth - depthAdj) * unit, (xywh[0] + xywh[2]) * unit, (xywh[1] + xywh[3]) * unit, (depth - depthAdj + .06) * unit, side);

                boxRenderer.setExteriorIcon(iconOn);
                boxRenderer.renderExterior(renderer, block, x, y, z, boxCoord[0], boxCoord[1], boxCoord[2], boxCoord[3], boxCoord[4], boxCoord[5], 0, cut[side - 2]);
            }
            else if (level >= 2) {
                double indStart = xywh[0] + block.indStart / unit;
                double indEnd = xywh[0] + block.indEnd / unit;
                double indCur = getIndEnd(block, tile, i, indStart, (block.indEnd - block.indStart) / unit);

                if (indCur > indStart) {
                    if (indCur >= indEnd)
                        indCur = xywh[0] + xywh[2];

                    setCoord(boxCoord, xywh[0] * unit, xywh[1] * unit, (depth - depthAdj) * unit, indCur * unit, (xywh[1] + xywh[3]) * unit, (depth - depthAdj + .06) * unit, side);
                    if (side == 2 || side == 5)
                        renderer.flipTexture = true;

                    boxRenderer.setExteriorIcon(iconOn);
                    boxRenderer.renderExterior(renderer, block, x, y, z, boxCoord[0], boxCoord[1], boxCoord[2], boxCoord[3], boxCoord[4], boxCoord[5], 0, cut[side - 2]);

                    renderer.flipTexture = false;
                }
            }
        }
    }

    private double getIndEnd (BlockDrawers block, TileEntityDrawers tile, int slot, double x, double w) {
        IDrawer drawer = tile.getDrawer(slot);
        if (drawer == null)
            return x;

        int cap = drawer.getMaxCapacity();
        int count = drawer.getStoredItemCount();
        if (cap == 0 || count == 0)
            return x;

        int step = block.indSteps > 0 ? block.indSteps : 1000;
        float fillAmt = (float)(step * count / cap) / step;

        return x + (w * fillAmt);
    }

    private void setCoord (double[] coords, double xMin, double yMin, double zMin, double xMax, double yMax, double zMax) {
        coords[0] = xMin;
        coords[1] = yMin;
        coords[2] = zMin;
        coords[3] = xMax;
        coords[4] = yMax;
        coords[5] = zMax;
    }

    private void setCoord (double[] coords, double xMin, double yMin, double zMin, double xMax, double yMax, double zMax, int side) {
        setCoord(coords, xMin, yMin, zMin, xMax, yMax, zMax);
        transformCoord(coords, side);
    }

    private void transformCoord (double[] coords, int side) {
        double tmpX, tmpZ;

        switch (side) {
            case 2:
                tmpX = coords[0];
                coords[0] = 1 - coords[3];
                coords[3] = 1 - tmpX;

                tmpZ = coords[2];
                coords[2] = 1 - coords[5];
                coords[5] = 1 - tmpZ;
                return;
            case 3:
                return;
            case 4:
                tmpX = coords[0];
                tmpZ = coords[3];
                coords[0] = 1 - coords[5];
                coords[3] = 1 - coords[2];

                coords[2] = tmpX;
                coords[5] = tmpZ;
                return;
            case 5:
                tmpX = coords[0];
                tmpZ = coords[3];
                coords[0] = coords[2];
                coords[3] = coords[5];

                coords[2] = 1 - tmpZ;
                coords[5] = 1 - tmpX;
                return;
        }
    }

    private void renderExterior (BlockDrawers block, int x, int y, int z, int side, RenderBlocks renderer) {
        double depth = block.halfDepth ? .5 : 1;
        double xMin = 0, xMax = 0, zMin = 0, zMax = 0;

        switch (side) {
            case 2:
                xMin = 0; xMax = 1;
                zMin = 1 - depth; zMax = 1;
                break;
            case 3:
                xMin = 0; xMax = 1;
                zMin = 0; zMax = depth;
                break;
            case 4:
                xMin = 1 - depth; xMax = 1;
                zMin = 0; zMax = 1;
                break;
            case 5:
                xMin = 0; xMax = depth;
                zMin = 0; zMax = 1;
                break;
        }

        boxRenderer.renderExterior(renderer, block, x, y, z, xMin, 0, zMin, xMax, 1, zMax, 0, ModularBoxRenderer.sideCut[side]);
    }

    private void renderInterior (BlockDrawers block, int x, int y, int z, int side, RenderBlocks renderer) {
        double unit = block.trimDepth;
        double depth = block.halfDepth ? .5 : 1;
        double xMin = 0, xMax = 0, zMin = 0, zMax = 0;

        switch (side) {
            case 2:
                xMin = unit; xMax = 1 - unit;
                zMin = 1 - depth; zMax = 1 - depth + unit;
                break;
            case 3:
                xMin = unit; xMax = 1 - unit;
                zMin = depth - unit; zMax = depth;
                break;
            case 4:
                xMin = 1 - depth; xMax = 1 - depth + unit;
                zMin = unit; zMax = 1 - unit;
                break;
            case 5:
                xMin = depth - unit; xMax = depth;
                zMin = unit; zMax = 1 - unit;
                break;
        }

        boxRenderer.renderInterior(renderer, block, x, y, z, xMin, unit, zMin, xMax, 1 - unit, zMax, 0, ModularBoxRenderer.sideCut[side]);
    }

    @Override
    public boolean shouldRender3DInInventory (int modelId) {
        return true;
    }

    @Override
    public int getRenderId () {
        return StorageDrawers.proxy.drawersRenderID;
    }
}
