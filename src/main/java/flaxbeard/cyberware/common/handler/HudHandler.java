package flaxbeard.cyberware.common.handler;

import java.util.List;
import java.util.Stack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

import flaxbeard.cyberware.Cyberware;
import flaxbeard.cyberware.api.CyberwareAPI;
import flaxbeard.cyberware.api.ICyberwareUserData;
import flaxbeard.cyberware.api.hud.CyberwareHudDataEvent;
import flaxbeard.cyberware.api.hud.CyberwareHudEvent;
import flaxbeard.cyberware.api.hud.IHudElement;
import flaxbeard.cyberware.api.hud.IHudElement.EnumAnchorHorizontal;
import flaxbeard.cyberware.api.hud.IHudElement.EnumAnchorVertical;
import flaxbeard.cyberware.api.hud.NotificationInstance;
import flaxbeard.cyberware.api.item.IHudjack;
import flaxbeard.cyberware.client.gui.GuiHudConfiguration;
import flaxbeard.cyberware.client.gui.hud.HudNBTData;
import flaxbeard.cyberware.client.gui.hud.MissingPowerDisplay;
import flaxbeard.cyberware.client.gui.hud.NotificationDisplay;
import flaxbeard.cyberware.client.gui.hud.PowerDisplay;

public class HudHandler
{
	public static final HudHandler INSTANCE = new HudHandler();
	
	
	// http://stackoverflow.com/a/16206356/1754640
	private static class NotificationStack<T> extends Stack<T>
	{
		private int maxSize;

		public NotificationStack(int size)
		{
			super();
			this.maxSize = size;
		}

		@Override
		public Object push(Object object)
		{
			while (this.size() >= maxSize)
			{
				this.remove(0);
			}
			return super.push((T) object);
		}
	}

	
	public static void addNotification(NotificationInstance not)
	{
		notifications.push(not);
	}
	
	public static final ResourceLocation HUD_TEXTURE = new ResourceLocation(Cyberware.MODID + ":textures/gui/hud.png");
	private static Iterable<ItemStack> inv;
	private static boolean lightArmor = false;
	public static Stack<NotificationInstance> notifications = new NotificationStack(5);
	private static int cachedCap = 0;
	private static int cachedTotal = 0;
	private static float cachedPercent = 0;
	private static int radioRange = -1;
	
	private static PowerDisplay pd = new PowerDisplay();
	private static MissingPowerDisplay mpd = new MissingPowerDisplay();
	private static NotificationDisplay nd = new NotificationDisplay();

	static
	{
		nd.setHorizontalAnchor(EnumAnchorHorizontal.LEFT);
		nd.setVerticalAnchor(EnumAnchorVertical.BOTTOM);
	}
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void addHudElements(CyberwareHudEvent event)
	{
		if (event.isHudjackAvailable())
		{
			event.addElement(pd);
			event.addElement(mpd);
			event.addElement(nd);

		}
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void saveHudElements(CyberwareHudDataEvent event)
	{
		event.addElement(pd);
		event.addElement(mpd);
		event.addElement(nd);
	}
	
	@SideOnly(Side.CLIENT)
	@SubscribeEvent
	public void onDrawScreenPost(RenderGameOverlayEvent.Post event)
	{
		if (event.getType() == ElementType.POTION_ICONS)
		{
			EntityPlayer p = Minecraft.getMinecraft().thePlayer;
			
			boolean active = false;
			if (CyberwareAPI.hasCapability(p))
			{
				List<ItemStack> hudjackItems = CyberwareAPI.getCapability(p).getHudjackItems();
				for (ItemStack stack : hudjackItems)
				{
					if (((IHudjack) CyberwareAPI.getCyberware(stack)).isActive(stack))
					{
						active = true;
						break;
					}
				}
			}
			
			CyberwareHudEvent hudEvent = new CyberwareHudEvent(event.getResolution(), active);
			MinecraftForge.EVENT_BUS.post(hudEvent);
			List<IHudElement> elements = hudEvent.getElements();
			boolean active2 = hudEvent.isHudjackAvailable();
			
			ScaledResolution sr = event.getResolution();
			for (IHudElement element : elements)
			{
				if (element.getHeight() + GuiHudConfiguration.getAbsoluteY(sr, element) <= 3)
				{
					GuiHudConfiguration.setYFromAbsolute(sr, element, 0 - element.getHeight() + 4);
				}
				
				if (GuiHudConfiguration.getAbsoluteY(sr, element) >= sr.getScaledHeight() - 3)
				{
					GuiHudConfiguration.setYFromAbsolute(sr, element, sr.getScaledHeight() - 4);
				}
				
				if (element.getWidth() + GuiHudConfiguration.getAbsoluteX(sr, element) <= 3)
				{
					GuiHudConfiguration.setXFromAbsolute(sr, element, 0 - element.getWidth() + 4);
				}
				
				if (GuiHudConfiguration.getAbsoluteX(sr, element) >= sr.getScaledWidth() - 3)
				{
					GuiHudConfiguration.setXFromAbsolute(sr, element, sr.getScaledWidth() - 4);
				}
				
				GL11.glPushMatrix();
				element.render(p, sr, active2, Minecraft.getMinecraft().currentScreen instanceof GuiHudConfiguration, event.getPartialTicks());
				GL11.glPopMatrix();
			}
				
				/*if (active)
				{
					float currTime = p.ticksExisted + event.getPartialTicks();
					
					GL11.glPushMatrix();
					GlStateManager.enableBlend();
					ICyberwareUserData data = CyberwareAPI.getCapability(p);
					
					Minecraft.getMinecraft().getTextureManager().bindTexture(HUD_TEXTURE);
			
					ScaledResolution res = event.getResolution();
					int left = 5;
					int top = 5;
					
					Iterable<ItemStack> currInv = p.getArmorInventoryList();
					if (currInv != inv)
					{
						inv = currInv;
						boolean temp = lightArmor;
						lightArmor = updateLightArmor();
						if (lightArmor != temp)
						{
							addNotification(new NotificationInstance(currTime, new NotificationArmor(lightArmor)));
						}
					}
		
					FontRenderer fr = Minecraft.getMinecraft().fontRendererObj;
					
					if (p.ticksExisted % 20 == 0)
					{
						cachedPercent = data.getPercentFull();
						cachedCap = data.getCapacity();
						cachedTotal = data.getStoredPower();
					}
		
					int temp = radioRange;
					radioRange = TileEntityBeacon.isInRange(p.worldObj, p.posX, p.posY, p.posZ);
					if (radioRange != temp)
					{
						addNotification(new NotificationInstance(currTime, new NotificationRadio(radioRange)));
					}
					
					float[] color = CyberwareAPI.getHUDColor();
					int colorHex = CyberwareAPI.getHUDColorHex();
					
					if (cachedPercent != -1)
					{
						int amount = Math.round((21F * cachedPercent));
		
						boolean danger = (cachedPercent <= .2F);
						boolean superDanger = danger && (cachedPercent <= .05F);
						int xOffset = (danger ? 39 : 0);
						
						if (!superDanger || p.ticksExisted % 4 != 0)
						{
							GlStateManager.pushMatrix();
							if (!danger) GlStateManager.color(color[0], color[1], color[2]);
							ClientUtils.drawTexturedModalRect(left, top, xOffset, 0, 13, 2 + (21 - amount));
							ClientUtils.drawTexturedModalRect(left, top + 2 + (21 - amount), 13 + xOffset, 2 + (21 - amount), 13, amount + 2);
							
							ClientUtils.drawTexturedModalRect(left, top + 2 + (21 - amount), 26 + xOffset, 2 + (21 - amount), 13, amount + 2);
							GlStateManager.popMatrix();
		
							fr.drawStringWithShadow(cachedTotal + " / " + cachedCap, left + 15, top + 8, danger ? 0xFF0000 : colorHex);
						}
						top += 28;
					}
					
					List<NotificationInstance> nTR = new ArrayList<NotificationInstance>();
					for (int i = 0; i < notifications.size(); i++)
					{
						NotificationInstance ni = notifications.get(i);
						INotification notification = ni.getNotification();
						if (currTime - ni.getCreatedTime() < notification.getDuration() + 25)
						{
							double pct = Math.max(0F, ((currTime - ni.getCreatedTime() - notification.getDuration()) / 30F));
			
							float move = (float) ((20 * Math.sin(pct * (Math.PI / 2F))));
							
							GL11.glPushMatrix();
							GL11.glColor3f(1.0F, 1.0F, 1.0F);
							GL11.glTranslatef(0F, move, 0F);
							int index = (notifications.size() - 1) - i;
							notification.render(5 + index * 18, res.getScaledHeight() - 5 - 14);
							GL11.glPopMatrix();
						}
						else
						{
							nTR.add(ni);
						}
					}
					
					for (NotificationInstance ni : nTR)
					{
						notifications.remove(ni);
					}
					
					RenderItem ir = Minecraft.getMinecraft().getRenderItem();
					List<ItemStack> stacks = data.getPowerOutages();
					List<Integer> stackTimes = data.getPowerOutageTimes();
					List<Integer> toRemove = new ArrayList<Integer>();
					left -= 1;
					float zL = ir.zLevel;
					ir.zLevel = -300;
					for (int i = stacks.size() - 1; i >= 0; i--)
					{
						ItemStack stack = stacks.get(i);
						if (stack != null)
						{
							int time = stackTimes.get(i);
							boolean keep = p.ticksExisted - time < 50;
							double pct = Math.max(0F, ((currTime - time - 20) / 30F));
		
							float move = (float) ((20 * Math.sin(pct * (Math.PI / 2F))));
							if (keep)
							{
								GL11.glPushMatrix();
								GL11.glTranslatef(-move, 0F, 0F);
								
								fr.drawStringWithShadow("!", left + 14, top + 8, 0xFF0000);
								
								RenderHelper.enableStandardItemLighting();
								ir.renderItemAndEffectIntoGUI(stack, left, top);
								RenderHelper.disableStandardItemLighting();
		
								GL11.glPopMatrix();
								top += 18;
							}
							else
							{
								toRemove.add(i);
							}
						}
					}
					ir.zLevel = zL;
					
					for (int i : toRemove)
					{
						stacks.remove(i);
						stackTimes.remove(i);
					}
					
					Minecraft.getMinecraft().getTextureManager().bindTexture(Gui.ICONS);
		
					GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
					GL11.glPopMatrix();
				}*/
			
		}
	}

}
