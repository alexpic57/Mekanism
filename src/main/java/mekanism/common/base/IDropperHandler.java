package mekanism.common.base;

import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTank;
import mekanism.common.item.ItemGaugeDropper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

public interface IDropperHandler 
{
	public Object[] getTanks();
	
	public static class DropperHandler
	{
		public static void useDropper(EntityPlayer player, Object tank, int button)
		{
			ItemStack stack = player.inventory.getItemStack();
			ItemGaugeDropper dropper = (ItemGaugeDropper)stack.getItem();
			
			System.out.println("use dropper " + player + " " + tank + " " + button + " " + stack);
			if(stack != null)
			{
				if(tank instanceof GasTank)
				{
					GasTank gasTank = (GasTank)tank;
					int dropperStored = dropper.getGas(stack) != null ? dropper.getGas(stack).amount : 0;
					
					if(dropper.getGas(stack) != null && !dropper.getGas(stack).isGasEqual(gasTank.getGas()))
					{
						return;
					}
					
					if(button == 0) //Insert gas into dropper
					{
						if(dropper.getFluid(stack) != null || gasTank.getGas() == null)
						{
							return;
						}
						
						int toInsert = Math.min(gasTank.getStored(), ItemGaugeDropper.CAPACITY-dropperStored);
						GasStack drawn = gasTank.draw(toInsert, true);
						dropper.setGas(stack, new GasStack(gasTank.getGasType(), dropperStored+(drawn != null ? drawn.amount : 0)));
					}
					else { //Extract gas from dropper
						if(dropper.getFluid(stack) != null || gasTank.getNeeded() == 0)
						{
							return;
						}
						
						int toExtract = Math.min(gasTank.getNeeded(), dropperStored);
						toExtract = gasTank.receive(new GasStack(dropper.getGas(stack).getGas(), toExtract), true);
						dropper.setGas(stack, new GasStack(dropper.getGas(stack).getGas(), dropperStored-toExtract));
					}
				}
				else if(tank instanceof FluidTank)
				{
					FluidTank fluidTank = (FluidTank)tank;
					int dropperStored = dropper.getFluid(stack) != null ? dropper.getFluid(stack).amount : 0;
					
					if(dropper.getFluid(stack) != null && !dropper.getFluid(stack).isFluidEqual(fluidTank.getFluid()))
					{
						return;
					}
					
					if(button == 0) //Insert fluid into dropper
					{
						if(dropper.getGas(stack) != null || fluidTank.getFluid() == null)
						{
							return;
						}
						
						int toInsert = Math.min(fluidTank.getFluidAmount(), ItemGaugeDropper.CAPACITY-dropperStored);
						FluidStack drawn = fluidTank.drain(toInsert, true);
						dropper.setFluid(stack, new FluidStack(fluidTank.getFluid().getFluid(), dropperStored+(drawn != null ? drawn.amount : 0)));
					}
					else { //Extract fluid from dropper
						if(dropper.getGas(stack) != null || fluidTank.getCapacity()-fluidTank.getFluidAmount() == 0)
						{
							return;
						}
						
						int toExtract = Math.min(fluidTank.getCapacity()-fluidTank.getFluidAmount(), dropperStored);
						toExtract = fluidTank.fill(new FluidStack(dropper.getFluid(stack).getFluid(), toExtract), true);
						dropper.setFluid(stack, new FluidStack(dropper.getFluid(stack).getFluid(), dropperStored-toExtract));
					}
				}
			}
		}
	}
}