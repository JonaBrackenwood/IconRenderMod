package be.brackenwood.iconrendermod;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_PACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glGetTexImage;
import static org.lwjgl.opengl.GL11.glPixelStorei;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.item.ItemStack;

import org.lwjgl.BufferUtils;
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
	private static ItemStack is = null, prevIs = null;
	public static List subItemList = new ArrayList();

	public RenderTicker()
	{
		mc = FMLClientHandler.instance().getClient();
		isRegistered = true;
	}

	@SubscribeEvent
	public void onTick(RenderTickEvent event)
	{
		if (event.phase.equals(Phase.START)) return;

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
				is = new ItemStack(GameData.itemRegistry.getObject((String) b),1,0);
				is.getItem().getSubItems(is.getItem(), null, subItemList);
				
		        if(is == null) continue;
	            try {
					sleep(100);
				} catch (InterruptedException e) {
					System.out.println("INTERRUPTED");
					e.printStackTrace();
				}
	            // If you only want to render one icon...
	            //break;
			}
			for(Object sis : subItemList) {
	            if(!isInGame) break;
	            is = (ItemStack) sis;
		        if(is == null) continue;
	            try {
					sleep(300);
				} catch (InterruptedException e) {
					System.out.println("INTERRUPTED");
					e.printStackTrace();
				}
	            //break;
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
    	glClearColor(0, 0, 0, 0);
		glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        GL11.glEnable(GL12.GL_RESCALE_NORMAL);
		RenderHelper.enableGUIStandardItemLighting();
        GL11.glScalef(31.22F, 31.22F, 31.22F);
        try
        {
        	itemRenderer.renderItemIntoGUI(mc.fontRenderer, mc.getTextureManager(), is, 0, 0);
        }
        catch(Exception e)
        {
            System.err.println(e.getMessage() + " while rendering: " + itemstack);
        }
        GL11.glScalef(0.1F, 0.1F, 0.1F);
		RenderHelper.disableStandardItemLighting();
        GL11.glDisable(GL12.GL_RESCALE_NORMAL);
        renderImage(itemstack);
    }
    
	/**
	 * Create the PNG image
	 */
    public static void renderImage(ItemStack currIs){
    	if (prevIs != currIs){
    		
    		// Based on the minecraft screenshot-helper
        	Framebuffer fbo = mc.getFramebuffer();
        	int width = fbo.framebufferTextureWidth;
        	int height = fbo.framebufferTextureHeight;
        	IntBuffer pixelBuffer = null;
    		int[] pixelValues = null;
        	
    		int k = width * height;

    		if (pixelBuffer == null || pixelBuffer.capacity() < k)
    		{
    			pixelBuffer = BufferUtils.createIntBuffer(k);
    			pixelValues = new int[k];
    		}

    		glPixelStorei(GL_PACK_ALIGNMENT, 1);
    		glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
    		pixelBuffer.clear();

    		// BGRA voor alpha channel
    		glBindTexture(GL_TEXTURE_2D, fbo.framebufferTexture);
    		glGetTexImage(GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);

    		pixelBuffer.get(pixelValues);
    		TextureUtil.func_147953_a(pixelValues, width, height);
    		BufferedImage bufferedimage = null;

            bufferedimage = new BufferedImage(fbo.framebufferWidth, fbo.framebufferHeight, 1);
            int l = fbo.framebufferTextureHeight - fbo.framebufferHeight;

            for (int i1 = l; i1 < fbo.framebufferTextureHeight; ++i1)
            {
                for (int j1 = 0; j1 < fbo.framebufferWidth; ++j1)
                {
                    bufferedimage.setRGB(j1, i1 - l, pixelValues[i1 * fbo.framebufferTextureWidth + j1]);
                }
            }

    		// Create directory and finally the png file
    		File file = new File(mc.mcDataDir, "renders");
    		file.mkdir();
    		try {
    			System.out.println(currIs.toString());
    			ImageIO.write(bufferedimage, "png", new File(file, GameData.itemRegistry.getNameForObject(is.getItem()).replace(":", "_") + "_" + currIs.getItemDamage() + ".png"));
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		prevIs = currIs;
    	}
    }
}