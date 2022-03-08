package com.github._5si.dancoremods.waterfov;

import dev.boredhuman.api.DanCoreAPI;
import dev.boredhuman.api.config.ConfigMinMax;
import dev.boredhuman.api.config.ConfigProperty;
import dev.boredhuman.api.events.BatchedLineRenderingEvent;
import dev.boredhuman.api.events.RenderHook;
import dev.boredhuman.api.module.AbstractModule;
import dev.boredhuman.api.module.ModColor;
import dev.boredhuman.api.module.Module;
import dev.boredhuman.api.util.MutableValue;
import dev.boredhuman.util.Pair;
import dev.boredhuman.util.Position;
import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.*;

//@Mod(modid = "watermod")
@Module(name = "Water", author = {"5si"}, description = "Cool settings for rendering water in minecraft!")
public class Main extends AbstractModule {

    final Minecraft mc = Minecraft.getMinecraft();

    @ConfigProperty(name = "Stop FOV Change In Water")
    MutableValue<Boolean> waterFOV = new MutableValue<>(true);

    @ConfigProperty(name = "Disable Water Fog")
    MutableValue<Boolean> waterFog = new MutableValue<>(true);

    @ConfigProperty(name = "Disable Water Overlay")
    MutableValue<Boolean> waterOverlay = new MutableValue<>(true);

    @ConfigProperty(name = "Dispenser Water Checker")
    MutableValue<Boolean> waterHelper = new MutableValue<>(false);

    @ConfigProperty(name = "Do Depth")
    MutableValue<Boolean> doDepth = new MutableValue<>(true);

    @ConfigProperty(name = "Color")
    ModColor waterHelperColor = new ModColor(0xFF0be0d9);

    @ConfigProperty(name = "Line Width")
    @ConfigMinMax(min = 1, max = 4)
    MutableValue<Float> lineWidth = new MutableValue<>(4.0F);

//    @Mod.EventHandler
//    public void postInit(FMLPostInitializationEvent e) {
//        init();
//    }

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

    private long tick = 0;
    Set<BlockPos> pos = new LinkedHashSet<>();

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent e) {
        if (mc.thePlayer == null || mc.theWorld == null || !this.isEnabled().getValue() || !this.waterHelper.getValue() || e.phase != TickEvent.Phase.END) {
            return;
        }
        if (++tick % 20 == 0) {
            pos.clear();
            BlockPos.MutableBlockPos mbp = new BlockPos.MutableBlockPos();
            for (int x = (int) (mc.thePlayer.posX - 25); x <= mc.thePlayer.posX + 25; ++x)
                for (int y = (int) (mc.thePlayer.posY - 25); y <= mc.thePlayer.posY + 25; ++y)
                    for (int z = (int) (mc.thePlayer.posZ - 25); z <= mc.thePlayer.posZ + 25; ++z) {
                        if (y < 1 || y > 255) continue;
                        mbp.set(x, y, z);
                        IBlockState bs = mc.theWorld.getBlockState(mbp);
                        if (!(bs.getBlock() instanceof BlockDispenser)) {
                            continue;
                        }
                        EnumFacing facing = bs.getValue(BlockDispenser.FACING);
                        BlockPos faceb = mbp.offset(facing);
                        IBlockState face = mc.theWorld.getBlockState(faceb);
                        int blockid = Block.getIdFromBlock(face.getBlock());
                        if (blockid == 8 || blockid == 9) {
                            continue;
                        }
                        BlockPos top = mbp.offset(EnumFacing.UP);
                        IBlockState abovedispenser = mc.theWorld.getBlockState(top);
                        BlockPos below = mbp.offset(EnumFacing.DOWN);
                        IBlockState belowdispenser = mc.theWorld.getBlockState(below);
                        if (abovedispenser instanceof BlockDispenser) continue;
                        if (belowdispenser.getBlock() instanceof BlockDispenser) {
                            BlockPos target = below.offset(facing);
                            IBlockState targetb = mc.theWorld.getBlockState(target);
                            BlockPos abovefront = target.offset(EnumFacing.UP);
                            IBlockState abovewater = mc.theWorld.getBlockState(abovefront);
                            int id = Block.getIdFromBlock(targetb.getBlock());
                            if (id == 8 || id == 9) {
                                continue;
                            }
                            if (abovewater.getBlock() instanceof BlockAir) {
                                pos.add(abovefront);
                            } else {
                                pos.add(target);
                            }
                        } else {
                            pos.add(faceb);
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
        for (BlockPos pos : this.pos) {
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


}
