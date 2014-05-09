package be.brackenwood.iconrendermod;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;

@Mod(modid = "iconrendermod", name="Icon Render Mod", version = "0.1.1")
public class IconRenderMod
{
	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		FMLCommonHandler.instance().bus().register(new RenderTicker());
	}
}