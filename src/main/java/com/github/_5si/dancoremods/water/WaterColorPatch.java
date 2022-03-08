package com.github._5si.dancoremods.water;

import dev.boredhuman.api.tweakers.BasicHookModule;
import dev.boredhuman.tweaker.BasicHook;

public class WaterColorPatch extends BasicHookModule {

    public WaterColorPatch(){
        super("net.minecraft.block.BlockLiquid", "colorMultiplier", "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/BlockPos;I)I", WATER_PATCH, BasicHook.Hook.HEAD);
    }

    public static String WATER_PATCH = "Water Color Patch";
}
