package mekanism.common;

import java.util.ArrayList;
import java.util.HashSet;

import mekanism.api.ITransmitter;
import mekanism.api.Object3D;
import mekanism.common.PacketHandler.Transmission;
import mekanism.common.network.PacketDataRequest;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;

import com.google.common.io.ByteArrayDataInput;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TileEntityLogisticalTransporter extends TileEntityTransmitter<InventoryNetwork> implements ITileNetwork
{
	/** This transporter's active state. */
	public boolean isActive = false;
	
	@Override
	public InventoryNetwork getNetwork(boolean createIfNull)
	{
		if(theNetwork == null && createIfNull)
		{
			TileEntity[] adjacentTransporters = CableUtils.getConnectedCables(this);
			HashSet<InventoryNetwork> connectedNets = new HashSet<InventoryNetwork>();
			
			for(TileEntity transporter : adjacentTransporters)
			{
				if(MekanismUtils.checkNetwork(transporter, InventoryNetwork.class) && ((ITransmitter<InventoryNetwork>)transporter).getNetwork(false) != null)
				{
					connectedNets.add(((ITransmitter<InventoryNetwork>)transporter).getNetwork());
				}
			}
			if(connectedNets.size() == 0 || worldObj.isRemote)
			{
				theNetwork = new InventoryNetwork(this);
			}
			else if(connectedNets.size() == 1)
			{
				theNetwork = connectedNets.iterator().next();
				theNetwork.transmitters.add(this);
			}
			else {
				theNetwork = new InventoryNetwork(connectedNets);
				theNetwork.transmitters.add(this);
			}
		}
		
		return theNetwork;
	}

	@Override
	public void fixNetwork()
	{
		getNetwork().fixMessedUpNetwork(this);
	}
	
	@Override
	public void invalidate()
	{
		if(!worldObj.isRemote)
		{
			getNetwork().split(this);
		}
		
		super.invalidate();
	}
	
	@Override
	public void removeFromNetwork()
	{
		if(theNetwork != null)
		{
			theNetwork.removeTransmitter(this);
		}
	}

	@Override
	public void refreshNetwork() 
	{
		if(!worldObj.isRemote)
		{
			for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
			{
				TileEntity tileEntity = Object3D.get(this).getFromSide(side).getTileEntity(worldObj);
				
				if(MekanismUtils.checkNetwork(tileEntity, InventoryNetwork.class))
				{
					getNetwork().merge(((ITransmitter<InventoryNetwork>)tileEntity).getNetwork());
				}
			}
			
			getNetwork().refresh();
		}
	}
	
	@Override
	public boolean canUpdate()
	{
		return false;
	}
	
	@Override
	public void validate()
	{
		super.validate();
		
		if(worldObj.isRemote)
		{
			PacketHandler.sendPacket(Transmission.SERVER, new PacketDataRequest().setParams(Object3D.get(this)));
		}
	}
	
	@Override
	public void handlePacketData(ByteArrayDataInput dataStream)
	{
		isActive = dataStream.readBoolean();
	}
	
	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		data.add(isActive);
		return data;
	}
	
	@Override
    public void readFromNBT(NBTTagCompound nbtTags)
    {
        super.readFromNBT(nbtTags);

        isActive = nbtTags.getBoolean("isActive");
    }

	@Override
    public void writeToNBT(NBTTagCompound nbtTags)
    {
        super.writeToNBT(nbtTags);
        
        nbtTags.setBoolean("isActive", isActive);
    }
	
	@Override
	@SideOnly(Side.CLIENT)
	public AxisAlignedBB getRenderBoundingBox()
	{
		return INFINITE_EXTENT_AABB;
	}
}
