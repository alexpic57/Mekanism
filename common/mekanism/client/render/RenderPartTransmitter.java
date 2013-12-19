package mekanism.client.render;

import java.util.HashMap;
import java.util.Map;

import mekanism.client.render.MekanismRenderer.DisplayInteger;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.common.multipart.PartLogisticalTransporter;
import mekanism.common.multipart.PartMechanicalPipe;
import mekanism.common.multipart.PartPressurizedTube;
import mekanism.common.multipart.PartSidedPipe;
import mekanism.common.multipart.PartSidedPipe.ConnectionType;
import mekanism.common.multipart.PartTransmitter;
import mekanism.common.multipart.PartUniversalCable;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.Fluid;

import org.lwjgl.opengl.GL11;

import codechicken.lib.colour.Colour;
import codechicken.lib.colour.ColourRGBA;
import codechicken.lib.lighting.LightModel;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.ColourMultiplier;
import codechicken.lib.render.IconTransformation;
import codechicken.lib.render.TextureUtils;
import codechicken.lib.render.TextureUtils.IIconRegister;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;

public class RenderPartTransmitter implements IIconRegister
{
	public static RenderPartTransmitter INSTANCE;
	
	public static Map<String, CCModel> small_models;
    public static Map<String, CCModel> large_models;
	public static Map<String, CCModel> contents_models;
	
	private static final int stages = 100;
	private static final double height = 0.45;
	private static final double offset = 0.015;
	
	private HashMap<ForgeDirection, HashMap<Fluid, DisplayInteger[]>> cachedLiquids = new HashMap<ForgeDirection, HashMap<Fluid, DisplayInteger[]>>();

	public static RenderPartTransmitter getInstance()
	{
		return INSTANCE;
	}
	
	public static void init()
	{
		INSTANCE = new RenderPartTransmitter();
		TextureUtils.addIconRegistrar(INSTANCE);
		
        small_models = CCModel.parseObjModels(MekanismUtils.getResource(ResourceType.MODEL, "transmitter_small.obj"), 7, null);

        for(CCModel c : small_models.values())
        {
            c.apply(new Translation(.5, .5, .5));
            c.computeLighting(LightModel.standardLightModel);
            c.shrinkUVs(0.0005);
        }

        large_models = CCModel.parseObjModels(MekanismUtils.getResource(ResourceType.MODEL, "transmitter_large.obj"), 7, null);

        for(CCModel c : large_models.values())
        {
            c.apply(new Translation(.5, .5, .5));
            c.computeLighting(LightModel.standardLightModel);
            c.shrinkUVs(0.0005);
        }

        contents_models = CCModel.parseObjModels(MekanismUtils.getResource(ResourceType.MODEL, "transmitter_contents.obj"), 7, null);
        
		for(CCModel c : contents_models.values())
        {
            c.apply(new Translation(.5, .5, .5));
            c.computeLighting(LightModel.standardLightModel);
            c.shrinkUVs(0.0005);
        }
	}
	
	private void push()
	{
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}
	
	private void pop()
	{
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}
	
	public void renderContents(PartUniversalCable cable, Vector3 pos)
	{
		if(cable.currentPower == 0)
		{
			return;
		}
		
		GL11.glPushMatrix();
		CCRenderState.reset();
		CCRenderState.useNormals(true);
		CCRenderState.useModelColours(true);
		CCRenderState.startDrawing(7);
		GL11.glTranslated(pos.x, pos.y, pos.z);
		
		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			if(cable.getConnectionType(side) != ConnectionType.NONE)
			{
				renderEnergySide(side, cable);
			}
		}
		
		MekanismRenderer.glowOn();
		CCRenderState.draw();
		MekanismRenderer.glowOff();
		
		GL11.glPopMatrix();
	}
	
	public void renderContents(PartMechanicalPipe pipe, Vector3 pos)
	{
		float targetScale = pipe.getTransmitterNetwork().fluidScale;
		
		if(Math.abs(pipe.currentScale - targetScale) > 0.01)
		{
			pipe.currentScale = (12 * pipe.currentScale + targetScale) / 13;
		}
		else {
			pipe.currentScale = targetScale;
		}
		
		Fluid fluid = pipe.getTransmitterNetwork().refFluid;
		float scale = pipe.currentScale;
		
		if(scale > 0.01 && fluid != null)
		{	
			push();
			
			MekanismRenderer.glowOn(fluid.getLuminosity());
			
			CCRenderState.changeTexture((MekanismRenderer.getBlocksTexture()));
			GL11.glTranslated(pos.x, pos.y, pos.z);
			
			boolean gas = fluid.isGaseous();
			
			for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
			{
				if(pipe.getConnectionType(side) != ConnectionType.NONE && PartTransmitter.connectionMapContainsSide(pipe.getAllCurrentConnections(), side))
				{
					DisplayInteger[] displayLists = getListAndRender(side, fluid);
					
					if(displayLists != null)
					{
						if(!gas)
						{
							displayLists[Math.max(3, (int)((float)scale*(stages-1)))].render();
						}
						else {
							GL11.glColor4f(1F, 1F, 1F, scale);
							displayLists[stages-1].render();
						}
					}
				}
			}
			
			DisplayInteger[] displayLists = getListAndRender(ForgeDirection.UNKNOWN, fluid);
			
			if(displayLists != null)
			{
				if(!gas)
				{
					displayLists[Math.max(3, (int)((float)scale*(stages-1)))].render();
				}
				else {
					GL11.glColor4f(1F, 1F, 1F, scale);
					displayLists[stages-1].render();
				}
			}
			
			MekanismRenderer.glowOff();
			
			pop();
		}
		
	}
	
	private DisplayInteger[] getListAndRender(ForgeDirection side, Fluid fluid)
	{
		if(side == null || fluid == null || fluid.getIcon() == null)
		{
			return null;
		}
		
		if(cachedLiquids.containsKey(side) && cachedLiquids.get(side).containsKey(fluid))
		{
			return cachedLiquids.get(side).get(fluid);
		}
		
		Model3D toReturn = new Model3D();
		toReturn.baseBlock = Block.waterStill;
		toReturn.setTexture(fluid.getIcon());
		
		toReturn.setSideRender(side, false);
		toReturn.setSideRender(side.getOpposite(), false);
		
		DisplayInteger[] displays = new DisplayInteger[stages];
		
		if(cachedLiquids.containsKey(side))
		{
			cachedLiquids.get(side).put(fluid, displays);
		}
		else {
			HashMap<Fluid, DisplayInteger[]> map = new HashMap<Fluid, DisplayInteger[]>();
			map.put(fluid, displays);
			cachedLiquids.put(side, map);
		}
		
		MekanismRenderer.colorFluid(fluid);
		
		for(int i = 0; i < stages; i++)
		{
			displays[i] = DisplayInteger.createAndStart();
			
			switch(side)
			{
				case UNKNOWN:
				{
					toReturn.minX = 0.25 + offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.25 + offset;
					
					toReturn.maxX = 0.75 - offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.75 - offset;
					break;
				}
				case DOWN:
				{
					toReturn.minX = 0.5 - (((float)i / (float)stages)*height)/2;
					toReturn.minY = 0.0;
					toReturn.minZ = 0.5 - (((float)i / (float)stages)*height)/2;
					
					toReturn.maxX = 0.5 + (((float)i / (float)stages)*height)/2;
					toReturn.maxY = 0.25 + offset;
					toReturn.maxZ = 0.5 + (((float)i / (float)stages)*height)/2;
					break;
				}
				case UP:
				{
					toReturn.minX = 0.5 - (((float)i / (float)stages)*height)/2;
					toReturn.minY = 0.25 - offset + ((float)i / (float)stages)*height;
					toReturn.minZ = 0.5 - (((float)i / (float)stages)*height)/2;
					
					toReturn.maxX = 0.5 + (((float)i / (float)stages)*height)/2;
					toReturn.maxY = 1.0;
					toReturn.maxZ = 0.5 + (((float)i / (float)stages)*height)/2;
					break;
				}
				case NORTH:
				{
					toReturn.minX = 0.25 + offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.0;
					
					toReturn.maxX = 0.75 - offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.25 + offset;
					break;
				}
				case SOUTH:
				{
					toReturn.minX = 0.25 + offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.75 - offset;
					
					toReturn.maxX = 0.75 - offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 1.0;
					break;
				}
				case WEST:
				{
					toReturn.minX = 0.0;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.25 + offset;
					
					toReturn.maxX = 0.25 + offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.75 - offset;
					break;
				}
				case EAST:
				{
					toReturn.minX = 0.75 - offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.25 + offset;
					
					toReturn.maxX = 1.0;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.75 - offset;
					break;
				}
			}
			
			MekanismRenderer.renderObject(toReturn);
			displays[i].endList();
		}
		
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		
		return displays;
	}

	public void renderContents(PartPressurizedTube tube, Vector3 pos)
	{
		if(tube.getTransmitterNetwork().refGas == null || tube.getTransmitterNetwork().gasScale == 0)
		{
			return;
		}
		
		GL11.glPushMatrix();
		CCRenderState.reset();
		CCRenderState.useNormals(true);
		CCRenderState.useModelColours(true);
		CCRenderState.startDrawing(7);
		GL11.glTranslated(pos.x, pos.y, pos.z);
		
		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			if(tube.getConnectionType(side) != ConnectionType.NONE)
			{
				renderGasSide(side, tube);
			}
		}
		
		CCRenderState.draw();
		
		GL11.glPopMatrix();
	}
	
	public void renderStatic(PartSidedPipe transmitter)
	{
		TextureUtils.bindAtlas(0);
		CCRenderState.reset();
		CCRenderState.useModelColours(true);
		CCRenderState.setBrightness(transmitter.world(), transmitter.x(), transmitter.y(), transmitter.z());
		
		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			renderSide(side, transmitter);
		}
	}
	
	public void renderSide(ForgeDirection side, PartSidedPipe transmitter)
	{
		boolean connected = PartTransmitter.connectionMapContainsSide(transmitter.getAllCurrentConnections(), side);
		Icon renderIcon = transmitter.getIconForSide(side);
		renderPart(renderIcon, transmitter.getModelForSide(side, false), transmitter.x(), transmitter.y(), transmitter.z());
	}

	public void renderEnergySide(ForgeDirection side, PartUniversalCable cable)
	{
		boolean connected = PartTransmitter.connectionMapContainsSide(cable.getAllCurrentConnections(), side);
		renderTransparency(MekanismRenderer.energyIcon, cable.getModelForSide(side, true), new ColourRGBA(1.0, 1.0, 1.0, cable.currentPower));
	}
	
	public void renderGasSide(ForgeDirection side, PartPressurizedTube tube)
	{
		boolean connected = PartTransmitter.connectionMapContainsSide(tube.getAllCurrentConnections(), side);
		renderTransparency(tube.getTransmitterNetwork().refGas.getIcon(), tube.getModelForSide(side, true), new ColourRGBA(1.0, 1.0, 1.0, tube.currentScale));
	}

    public void renderPart(Icon icon, CCModel cc, double x, double y, double z)
	{
        cc.render(0, cc.verts.length, new Translation(x, y, z), new IconTransformation(icon), null);
    }

    public void renderTransparency(Icon icon, CCModel cc, Colour colour) 
    {
        cc.render(0, cc.verts.length, new Translation(0, 0, 0), new IconTransformation(icon), new ColourMultiplier(colour));
    }

	@Override
	public void registerIcons(IconRegister register)
	{
		PartUniversalCable.registerIcons(register);
		PartMechanicalPipe.registerIcons(register);
		PartPressurizedTube.registerIcons(register);
		PartLogisticalTransporter.registerIcons(register);
	}

	@Override
	public int atlasIndex()
	{
		return 0;
	}
}
