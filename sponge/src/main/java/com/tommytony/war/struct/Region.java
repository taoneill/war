package com.tommytony.war.struct;

import org.spongepowered.api.block.Block;
import org.spongepowered.api.math.Vector3d;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.extent.BlockVolume;

/**
 * A selection of blocks in the world. Identified by two corners.
 */
public class Region implements BlockVolume {
    /**
     * One corner of the selection.
     */
    private Location first;
    /**
     * The second corner of the selection.
     */
    private Location second;

    public Region(Location first, Location second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Calculate the minimum value of the selection.
     *
     * @return the minimum value.
     */
    public Location getMin() {
        return new Location(first.getExtent(), first.getPosition().min(second.getPosition()));
    }

    /**
     * Calculate the maximum value of the selection.
     *
     * @return the maximum value.
     */
    public Location getMax() {
        return new Location(first.getExtent(), first.getPosition().max(second.getPosition()));
    }

    /**
     * Get the size of the region in the X dimension.
     *
     * @return X dimension length.
     */
    public int getSizeX() {
        return getMax().getBlock().getX() - getMin().getBlock().getX();
    }

    /**
     * Get the size of the region in the Y dimension.
     *
     * @return Y dimension length.
     */
    public int getSizeY() {
        return getMax().getBlock().getY() - getMin().getBlock().getY();
    }

    /**
     * Get the size of the region in the Z dimension.
     *
     * @return Z dimension length.
     */
    public int getSizeZ() {
        return getMax().getBlock().getZ() - getMin().getBlock().getZ();
    }

    /**
     * Get the total area of the region.
     *
     * @return region total area.
     */
    public int getSize() {
        return getSizeX() * getSizeY() * getSizeZ();
    }

    /**
     * Get a representation of the block at the given position.
     *
     * @param position The position
     * @return The block
     */
    @Override
    public Block getBlock(Vector3d position) {
        return first.getExtent().getBlock(position);
    }

    /**
     * Get a representation of the block at the given position.
     *
     * @param x The X position
     * @param y The Y position
     * @param z The Z position
     * @return The block
     */
    @Override
    public Block getBlock(int x, int y, int z) {
        return first.getExtent().getBlock(x, y, z);
    }
}
