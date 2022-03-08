package com.github._5si.dancoremods.water;

import dev.boredhuman.api.DanCoreAPI;
import dev.boredhuman.api.config.CategoryStart;
import dev.boredhuman.api.config.ConfigMinMax;
import dev.boredhuman.api.config.ConfigProperty;
import dev.boredhuman.api.events.BatchedLineRenderingEvent;
import dev.boredhuman.api.events.RenderHook;
import dev.boredhuman.api.module.AbstractModule;
import dev.boredhuman.api.module.ModColor;
import dev.boredhuman.api.module.Module;
import dev.boredhuman.api.util.MutableValue;
import dev.boredhuman.gui.elements.BasicElement;
import dev.boredhuman.gui.listeners.ClickListener;
import dev.boredhuman.tweaker.BasicHook;
import dev.boredhuman.tweaker.ReturnStrategy;
import dev.boredhuman.util.Pair;
import dev.boredhuman.util.Position;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;

import java.util.*;
import java.util.List;

//@Mod(modid = "watermod")
@Module(name = "Water", author = {"5si"}, description = "Cool settings for rendering water in minecraft!")
public class Main extends AbstractModule {

    final Minecraft mc = Minecraft.getMinecraft();

    @ConfigProperty(name = "Stop FOV Change In Water")
    MutableValue<Boolean> waterFOV = new MutableValue<>(true);

    @ConfigProperty(name = "Disable Fog")
    MutableValue<Boolean> waterFog = new MutableValue<>(true);

    @ConfigProperty(name = "Disable Water Overlay")
    MutableValue<Boolean> waterOverlay = new MutableValue<>(true);

    @ConfigProperty(name = "Custom Water Tint")
    MutableValue<Boolean> doWaterTint = new MutableValue<>(false);

    @ConfigProperty(name = "Water Tint")
    ModColor waterTint = new ModColor(16777215);

    @ConfigProperty(name = "Custom Lava Tint")
    MutableValue<Boolean> doLavaTint = new MutableValue<>(false);

    @ConfigProperty(name = "Lava Tint")
    ModColor lavaTint = new ModColor(16777215);

    @ConfigProperty(name = "Apply Colors")
    ClickListener clickListener = this::applyColors;

    @CategoryStart(name = "Dispenser Water checker")
    @ConfigProperty(name = "Enabled")
    MutableValue<Boolean> waterHelper = new MutableValue<>(false);

    @ConfigProperty(name = "Radius")
    @ConfigMinMax(min = 1, max = 100)
    MutableValue<Integer> waterRadius = new MutableValue<>(48);

    @ConfigProperty(name = "Ticks Delay Between Checks")
    @ConfigMinMax(min = 1, max = 100)
    MutableValue<Integer> waterCheckTime = new MutableValue<>(32);

    @ConfigProperty(name = "Do Depth")
    MutableValue<Boolean> doDepth = new MutableValue<>(true);

    @ConfigProperty(name = "Color")
    ModColor waterHelperColor = new ModColor(0xFF0be0d9);

    @ConfigProperty(name = "Line Width")
    @ConfigMinMax(min = 1, max = 4)
    MutableValue<Float> lineWidth = new MutableValue<>(4.0F);

    //    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent e) {
        init();
    }

    private boolean onOptifine;

    public Main() {
        BasicHook.addFunction(array -> {
            if (!this.isEnabled().getValue() || (!this.doWaterTint.getValue() && !this.doLavaTint.getValue())) {
                return new ReturnStrategy();
            }
            IBlockAccess worldIn = (IBlockAccess) array[1];
            BlockPos pos = (BlockPos) array[2];
            Material material = worldIn.getBlockState(pos).getBlock().getMaterial();
            if (material == Material.water && doWaterTint.getValue()) {
                return new ReturnStrategy(waterTint.getIntColor());
            } else if (material == Material.lava && doLavaTint.getValue()) {
                return new ReturnStrategy(lavaTint.getIntColor());
            }
            return new ReturnStrategy();
        }, WaterColorPatch.WATER_PATCH);
        try {
            Class<?> klass = Class.forName("net.optifine.Log");
            onOptifine = true;
        } catch (Throwable ignored) {
            onOptifine = false;
        }
        if (onOptifine) {

            BasicHook.addFunction(array -> {
                if (!this.isEnabled().getValue() || (!this.doWaterTint.getValue() && !this.doLavaTint.getValue())) {
                    return new ReturnStrategy();
                }
                IBlockState blockState = (IBlockState) array[1];
                System.out.println(blockState.getBlock().getUnlocalizedName());
                Material material = blockState.getBlock().getMaterial();
                if (material == Material.water) {
                    if (this.doWaterTint.getValue()) {
                        return new ReturnStrategy(this.waterTint.getIntColor());
                    } else return new ReturnStrategy();
                } else if (material == Material.lava) {
                    if (this.doLavaTint.getValue()) {
                        return new ReturnStrategy(this.lavaTint.getIntColor());
                    } else return new ReturnStrategy();
                }
                return new ReturnStrategy();
            }, WaterColorOptifinePatch.WATER_PATCH_OPTIFINE);
        }
    }

    @Override
    public void init() {
        DanCoreAPI.getAPI().registerModules(this);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void fov(EntityViewRenderEvent.FOVModifier e) {
        if (this.isEnabled().getValue() && waterFOV.getValue()) {
            Block block = ActiveRenderInfo.getBlockAtEntityViewpoint(this.mc.theWorld, mc.thePlayer, (float) e.renderPartialTicks);
            if (block.getMaterial() == Material.water) {
                e.setFOV(e.getFOV() / (60.0F / 70.0F));
            }
        }
    }

    @SubscribeEvent
    public void fog(EntityViewRenderEvent.FogDensity e) {
        if (this.isEnabled().getValue() && waterFog.getValue()) {
            e.density = 0F;
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void overlay(RenderBlockOverlayEvent e) {
        if (this.isEnabled().getValue() && waterOverlay.getValue()) {
            if (e.overlayType == RenderBlockOverlayEvent.OverlayType.WATER) {
                e.setCanceled(true);
            }
        }
    }

    private long tick = -1;
    final Set<BlockPos> positions = new ConcurrentSet<>();
    int delay = 20; //

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null || !this.isEnabled().getValue() || !this.waterHelper.getValue()) {
            positions.clear();
            return;
        }
        if (e.phase == TickEvent.Phase.END) {
            return;
        }
        if (++tick % delay != 0) return; // im actually a reject and forgot the "++" in "++tick" and didnt see it until 2 hours after debugging holy shit
        delay = waterCheckTime.getValue();
        positions.clear();
        int radius = waterRadius.getValue();
        int i = MathHelper.floor_double(mc.thePlayer.posX - radius);
        int j = MathHelper.floor_double(mc.thePlayer.posX + radius + 1.0D);
        int k = MathHelper.floor_double(mc.thePlayer.posY - radius);
        int l = MathHelper.floor_double(mc.thePlayer.posY + radius + 1.0D);
        int i1 = MathHelper.floor_double(mc.thePlayer.posZ - radius);
        int j1 = MathHelper.floor_double(mc.thePlayer.posZ + radius + 1.0D);
        int ystart = ((k - 1) < 0) ? 0 : (k - 1);

        for (int chunkx = (i >> 4); chunkx <= ((j - 1) >> 4); chunkx++) { // poggers spigot code
            int cx = chunkx << 4;
            for (int chunkz = (i1 >> 4); chunkz <= ((j1 - 1) >> 4); chunkz++) {
                int finalChunkz = chunkz;
                int finalChunkx = chunkx;
                Chunk chunk = mc.theWorld.getChunkFromChunkCoords(finalChunkx, finalChunkz);
                int cz = finalChunkz << 4;

                int xstart = (i < cx) ? cx : i;
                int xend = (j < (cx + 16)) ? j : (cx + 16);
                int zstart = (i1 < cz) ? cz : i1;
                int zend = (j1 < (cz + 16)) ? j1 : (cz + 16);
                for (int x = xstart; x < xend; x++) {
                    for (int z = zstart; z < zend; z++) {
                        BlockPos.MutableBlockPos mbp = new BlockPos.MutableBlockPos();
                        Map<EnumFacing, BlockPos> posToAdd = new HashMap<>();
                        for (int y = ystart; y < l; y++) {
                            mbp.set(x, y, z);
                            IBlockState blockState = chunk.getBlockState(mbp);
                            if (!(blockState.getBlock() instanceof BlockDispenser)) {
                                continue;
                            }
                            EnumFacing facing = blockState.getValue(BlockDispenser.FACING);
                            BlockPos faceBlock = mbp.offset(facing);
                            int id = Block.getIdFromBlock(chunk.getBlockState(faceBlock).getBlock());
                            if (id == 8 || id == 9) {
                                continue;
                            }
                            if (facing == EnumFacing.DOWN || facing == EnumFacing.UP) {
                                this.positions.add(faceBlock);
                                continue;
                            }
                            posToAdd.put(facing, faceBlock);
                        }
                        this.positions.addAll(posToAdd.values());
                    }
                }
            }
        }
    }

    RenderHook renderHook = phase -> {
        if (!this.doDepth.getValue()) {
            if (phase == RenderHook.Phase.SETUP) {
                GlStateManager.disableDepth();
            } else {
                GlStateManager.enableDepth();
            }
        }
    };

    @SubscribeEvent
    public void render(BatchedLineRenderingEvent e) {
        if (!this.isEnabled().getValue() || !this.waterHelper.getValue()) return;
        List<Pair<Position, Position>> lines = new ArrayList<>();
        for (BlockPos pos : this.positions) {
            lines.addAll(this.makeOutline(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1));
        }
        e.addLineLineWidth(Pair.of(this.lineWidth.getValue(), this.renderHook), new BatchedLineRenderingEvent.Line(lines, this.waterHelperColor.getBGRA()));
    }

    public List<Pair<Position, Position>> makeOutline(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        List<Pair<Position, Position>> linesList = new ArrayList<>();
        Position origin = new Position(minX, minY, minZ);
        Position max = new Position(maxX, maxY, maxZ);
        Position originX = new Position(maxX, minY, minZ);
        Position originZ = new Position(minX, minY, maxZ);
        Position originXZ = new Position(maxX, minY, maxZ);
        Position maxx = new Position(minX, maxY, maxZ);
        Position maxz = new Position(maxX, maxY, minZ);
        Position maxxz = new Position(minX, maxY, minZ);
        linesList.add(new Pair<>(origin, originX));
        linesList.add(new Pair<>(origin, originZ));
        linesList.add(new Pair<>(originX, originXZ));
        linesList.add(new Pair<>(originZ, originXZ));

        linesList.add(new Pair<>(max, maxx));
        linesList.add(new Pair<>(max, maxz));
        linesList.add(new Pair<>(maxx, maxxz));
        linesList.add(new Pair<>(maxz, maxxz));

        linesList.add(new Pair<>(origin, maxxz));
        linesList.add(new Pair<>(originX, maxz));
        linesList.add(new Pair<>(originZ, maxx));
        linesList.add(new Pair<>(originXZ, max));

        return linesList;
    }

    public boolean applyColors(BasicElement<?> basicElement) {
        Minecraft.getMinecraft().renderGlobal.loadRenderers();
        return true;
    }

}
