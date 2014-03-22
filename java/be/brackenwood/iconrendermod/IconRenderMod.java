package be.brackenwood.iconrendermod;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.item.ItemStack;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.EXTFramebufferObject.*;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.registry.GameData;

@Mod(modid = "iconrendermod", name="Icon Render Mod", version = "0.1.0")
public class IconRenderMod
{
	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		// On-screen block rendering
		FMLCommonHandler.instance().bus().register(new RenderTicker());
	}
	
	@EventHandler
	public void postInit(FMLInitializationEvent event)
	{
		// Render blocks in file
		// Creates about 315 images in vanilla, takes some time to complete
		createImages();
	}

	public static void createImages() {
		Minecraft mc = FMLClientHandler.instance().getClient();
		RenderItem itemRenderer = new RenderItem();
		int size = 500; // ImageSize
		ItemStack is;
		IntBuffer pixelBuffer = null;
		int[] pixelValues = null;

		for(Object b : GameData.itemRegistry.getKeys()) {
			// Create itemstack object out of b
			is = new ItemStack(GameData.itemRegistry.getObject((String) b),1,0);

			// Clear some buffers
			glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			
			// Create and bind framebuffer
			int fbo = glGenFramebuffersEXT();
			glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, fbo);

			// Create and bind depthbuffer
			int drb = glGenRenderbuffersEXT();
			glBindRenderbufferEXT(GL_RENDERBUFFER_EXT, drb);
			glRenderbufferStorageEXT(GL_RENDERBUFFER_EXT, GL_DEPTH_COMPONENT, size, size);
			glFramebufferRenderbufferEXT(GL_FRAMEBUFFER_EXT, GL_DEPTH_ATTACHMENT_EXT, GL_RENDERBUFFER_EXT, drb);

			// Create and bind texture
			int tex = glGenTextures();
			glBindTexture(GL_TEXTURE_2D, tex);

			// Specify a texture image
			glTexImage2D(GL_TEXTURE_2D, 0,GL_RGBA8, size, size, 0,GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer)null);

			// Bind texture to framebuffer
			glFramebufferTexture2DEXT(GL_FRAMEBUFFER_EXT, GL_COLOR_ATTACHMENT0_EXT, GL_TEXTURE_2D, tex, 0);
			
			// Render a Minecraft block or item
			// Commented out code is used for lighting and size
			glLoadIdentity();
	        glEnable(GL12.GL_RESCALE_NORMAL);
			//RenderHelper.enableGUIStandardItemLighting();
	        //glScalef(10F, 10F, 10F);
			itemRenderer.renderItemIntoGUI(mc.fontRenderer, mc.getTextureManager(), is, 0, 0);
			//glScalef(0.1F, 0.1F, 0.1F);
			//RenderHelper.disableStandardItemLighting();
	        glDisable(GL12.GL_RESCALE_NORMAL);
			
			/* Draw a rectangle
			glBegin(GL_TRIANGLES);
			glColor3f(1.0f, 0.0f, 0.0f);
			glVertex3f(0, 0, 0);
			glVertex3f(1, 0, 0);
			glVertex3f(0, 1, 0);
			glEnd();
			*/

			// Unbind framebuffer
			glBindFramebufferEXT(GL_FRAMEBUFFER_EXT, 0);

			// Based on the minecraft screenshot-helper
			int k = size * size;

			if (pixelBuffer == null || pixelBuffer.capacity() < k)
			{
				pixelBuffer = BufferUtils.createIntBuffer(k);
				pixelValues = new int[k];
			}

			glPixelStorei(GL_PACK_ALIGNMENT, 1);
			glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
			pixelBuffer.clear();

			glBindTexture(GL_TEXTURE_2D, tex);
			glGetTexImage(GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);

			pixelBuffer.get(pixelValues);
			TextureUtil.func_147953_a(pixelValues, size, size);
			BufferedImage bufferedimage = null;

			bufferedimage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

			for (int i1 = 0; i1 < size; ++i1)
			{
				for (int j1 = 0; j1 < size; ++j1)
				{
					bufferedimage.setRGB(j1, i1, pixelValues[i1 * size + j1]);
				}
			}

			// Create directory and finally the png file
			File file = new File(mc.mcDataDir, "renders");
			file.mkdir();
			try {
				ImageIO.write(bufferedimage, "png", new File(file, GameData.itemRegistry.getNameForObject(is.getItem()).replace(":", "_").concat(".png")));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			// For generating only one image, we break the for loop.
			//break;
		}



	}
}