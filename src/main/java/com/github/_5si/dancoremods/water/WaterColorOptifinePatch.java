package com.github._5si.dancoremods.water;

import dev.boredhuman.api.tweakers.BasicHookModule;
import dev.boredhuman.tweaker.BasicHook;

public class WaterColorOptifinePatch extends BasicHookModule {

    public WaterColorOptifinePatch(){
        super("net.optifine.CustomColors", "getFluidColor", "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/BlockPos;Lnet/optifine/render/RenderEnv;)I", WATER_PATCH_OPTIFINE, BasicHook.Hook.HEAD);
    }

    public static String WATER_PATCH_OPTIFINE = "Water Optifine Patch";

}
