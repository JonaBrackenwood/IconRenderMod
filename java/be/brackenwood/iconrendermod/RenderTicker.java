package be.brackenwood.iconrendermod;

import java.util.HashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.RenderTickEvent;
import cpw.mods.fml.common.registry.GameData;

public class RenderTicker {
	private static Minecraft mc;
	private static RenderItem itemRenderer = new RenderItem();
	private static boolean isRegistered = false;
	private static boolean isInGame = false;
	private static ItemStack is;

	public RenderTicker()
	{
		mc = FMLClientHandler.instance().getClient();
		isRegistered = true;
	}

	@SubscribeEvent
	public void onTick(RenderTickEvent event)
	{
		if (event.phase.equals(Phase.START))
			return;

		if (!onTickInGame(mc))
		{
			FMLCommonHandler.instance().bus().unregister(this);
			isRegistered = false;
		}
	}
	
	/**
	 * Check every tick if the player is in a game and render the active block
	 */
	public static boolean onTickInGame(Minecraft mc){
		if ((mc.inGameHasFocus || mc.currentScreen == null || (mc.currentScreen instanceof GuiChat)) && !mc.gameSettings.showDebugInfo && !mc.gameSettings.keyBindPlayerList.isPressed())
		{
			if(!isInGame){
				isInGame = true;
				new ThreadBlockLoop().start();
			}
			if (is != null) drawItem(is);
		}else{
			isInGame = false;
		}
		return true;
	}
	
	/**
	 * Sets the next active itemstack every second
	 */
	public static class ThreadBlockLoop extends Thread
	{
		@Override
		public void run(){
			for(Object b : GameData.itemRegistry.getKeys()) {
	            if(!isInGame) break;
				System.out.println((String) b);
				is = new ItemStack(GameData.itemRegistry.getObject((String) b),1,0);
		        if(is == null) continue;
	            try {
					sleep(1000);
				} catch (InterruptedException e) {
					System.out.println("INTERRUPTED");
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * Render the active itemstack with correct lighting
	 */
	private static int modelviewDepth = -1;
    private static HashSet<String> stackTraces = new HashSet<String>();
    public static void drawItem(ItemStack itemstack)
    {
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		RenderHelper.enableGUIStandardItemLighting();
        GL11.glScalef(10F, 10F, 10F);
        try
        {
        	itemRenderer.renderItemIntoGUI(mc.fontRenderer, mc.getTextureManager(), is, 2, 2);
        }
        catch(Exception e)
        {
            System.err.println(e.getMessage() + " while rendering: " + itemstack);
        }
        GL11.glScalef(0.1F, 0.1F, 0.1F);
		RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
    }
}