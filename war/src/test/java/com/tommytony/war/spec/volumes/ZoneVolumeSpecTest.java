package com.tommytony.war.spec.volumes;

import java.util.ArrayList;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import bukkit.tommytony.war.War;

import com.tommytony.war.*;
import com.tommytony.war.volumes.*;

public class ZoneVolumeSpecTest {

	// setNorthwest

	@Test
	public void setNorthwest_whenCreatingAndNoCornersAreSet_shouldSetCorner1AtTop() throws NotNorthwestException, TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(0);
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(0);
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		volume.setNorthwest(blockMock);

		// Assert
		assertEquals(null, volume.getCornerTwo());
		assertEquals(0, volume.getCornerOne().getX());
		assertEquals(127, volume.getCornerOne().getY()); // the corner should shoot up to the top
		assertEquals(0, volume.getCornerOne().getZ());
		assertEquals(10, volume.getCornerOne().getTypeId());
		assertEquals((byte)2, volume.getCornerOne().getData());
	}

	@Test
	public void setNorthwest_whenCreating_AndNoCorner1IsSet_ButCorner2Set_AndNewCornerBlockIsToEastOfCorner2_shouldThrowNotNorthwestException() throws TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(-64); // further north
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(-64);	// further east
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner2 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		boolean failed = false;
		try {
			volume.setNorthwest(blockMock);
		}
		catch(NotNorthwestException e) {
			failed = true;
		}

		// Assert
		// first corner shouldn't move
		assertTrue(failed);
		assertEquals(null, volume.getCornerOne());
		assertEquals(existingCorner2, volume.getCornerTwo());
	}

	@Test
	public void setNorthwest_whenCreating_AndNoCorner1IsSet_ButCorner2Set_AndNewCornerBlockIsToSouthOfCorner2_shouldThrowNotNorthwestException() throws TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(64); // further south
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(64);	// further west
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner2 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		boolean failed = false;
		try {
			volume.setNorthwest(blockMock);
		}
		catch(NotNorthwestException e) {
			failed = true;
		}

		// Assert
		// first corner shouldn't move
		assertTrue(failed);
		assertEquals(null, volume.getCornerOne());
		assertEquals(existingCorner2, volume.getCornerTwo());
	}

	@Test
	public void setNorthwest_whenCreating_AndNoCorner1IsSet_ButCorner2Set_AndNewCornerBlockIsTooCloseToCorner2_shouldThrowTooSmallException() throws NotNorthwestException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(-5); // further south
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(5);	// further west
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner2 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		boolean failed = false;
		try {
			volume.setNorthwest(blockMock);
		}
		catch(TooSmallException e) {
			failed = true;
		}

		// Assert
		// first corner shouldn't move
		assertTrue(failed);
		assertEquals(null, volume.getCornerOne());
		assertEquals(existingCorner2, volume.getCornerTwo());
	}

	@Test
	public void setNorthwest_whenCreating_AndNoCorner1IsSet_ButCorner2Set_AndNewCornerBlockIsTooFarFromCorner2_shouldThrowTooBigException() throws NotNorthwestException, TooSmallException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(-1000); // further south
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(1000);	// further west
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner2 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		boolean failed = false;
		try {
			volume.setNorthwest(blockMock);
		}
		catch(TooBigException e) {
			failed = true;
		}

		// Assert
		// first corner shouldn't move
		assertTrue(failed);
		assertEquals(null, volume.getCornerOne());
		assertEquals(existingCorner2, volume.getCornerTwo());
	}

	@Test
	public void setNorthwest_whenCreatingAndCorner1AlreadySet_shouldSetCorner2AtTop() throws NotNorthwestException, TooSmallException, TooBigException{  // nw always goes to top
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(-64); // further north
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(64);	// further west
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		volume.setNorthwest(blockMock);

		// Assert
		// first corner shouldn't move
		assertEquals(0, volume.getCornerOne().getX());
		assertEquals(64, volume.getCornerOne().getY());
		assertEquals(0, volume.getCornerOne().getZ());
		assertEquals(4, volume.getCornerOne().getTypeId());
		assertEquals((byte)4, volume.getCornerOne().getData());

		assertEquals(-64, volume.getCornerTwo().getX());
		assertEquals(127, volume.getCornerTwo().getY()); // the new corner should shoot up to the top
		assertEquals(64, volume.getCornerTwo().getZ());
		assertEquals(10, volume.getCornerTwo().getTypeId());
		assertEquals((byte)2, volume.getCornerTwo().getData());
	}

	@Test
	public void setNorthwest_whenCreating_AndCorner1AlreadySet_ButNewCornerBlockIsEastOfCorner1_shouldThrowNotNorthwestException() throws TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(-64); // further north
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(-64);	// further east
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		boolean failed = false;
		try {
			volume.setNorthwest(blockMock);
		}
		catch(NotNorthwestException e) {
			failed = true;
		}

		// Assert
		// first corner shouldn't move
		assertTrue(failed);
		assertEquals(null, volume.getCornerTwo());
		assertEquals(existingCorner1, volume.getCornerOne());
	}

	@Test
	public void setNorthwest_whenCreating_AndCorner1AlreadySet_ButNewCornerBlockIsSouthOfCorner1_shouldThrowNotNorthwestException() throws TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(64); // further south
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(64);	// further west
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		boolean failed = false;
		try {
			volume.setNorthwest(blockMock);
		}
		catch(NotNorthwestException e) {
			failed = true;
		}

		// Assert
		// first corner shouldn't move
		assertTrue(failed);
		assertEquals(null, volume.getCornerTwo());
		assertEquals(existingCorner1, volume.getCornerOne());
	}

	@Test
	public void setNorthwest_whenChangingVolumeWithCorner1NwCorner2Se_shouldMoveCorner1() throws NotNorthwestException, TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(-64);	// further north
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(64);	// further west
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(-32, 32, 32, 2, (byte)2);	// corner 1 at minX and maxZ (nw)
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		BlockInfo existingCorner2 = new BlockInfo(32, 96, -32, 4, (byte)4); // corner 2 at maxX and minZ (se)
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		volume.setNorthwest(blockMock);

		// Assert
		// first corner should move but not along y
		assertEquals(-64, volume.getCornerOne().getX());
		assertEquals(32, volume.getCornerOne().getY());
		assertEquals(64, volume.getCornerOne().getZ());
		assertEquals(2, volume.getCornerOne().getTypeId());
		assertEquals((byte)2, volume.getCornerOne().getData());

		// second corner shouldn't move
		assertEquals(32, volume.getCornerTwo().getX());
		assertEquals(96, volume.getCornerTwo().getY()); // moving an existing corner shouldn't change its height
		assertEquals(-32, volume.getCornerTwo().getZ());
		assertEquals(4, volume.getCornerTwo().getTypeId());
		assertEquals((byte)4, volume.getCornerTwo().getData());
	}

	@Test
	public void setNorthwest_whenChangingVolumeWithCorner1SeCorner2Nw_shouldMoveCorner2() throws NotNorthwestException, TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(-64);	// further north
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(64);	// further west
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(32, 32, -32, 2, (byte)2);	// corner 1 at maxX and minZ (se)
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		BlockInfo existingCorner2 = new BlockInfo(-32, 96, 32, 4, (byte)4); // corner 2 at minX and maxZ (nw)
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		volume.setNorthwest(blockMock);

		// Assert
		// first corner shouldn't move
		assertEquals(32, volume.getCornerOne().getX());
		assertEquals(32, volume.getCornerOne().getY());
		assertEquals(-32, volume.getCornerOne().getZ());
		assertEquals(2, volume.getCornerOne().getTypeId());
		assertEquals((byte)2, volume.getCornerOne().getData());

		// second corner should move but not along y
		assertEquals(-64, volume.getCornerTwo().getX());
		assertEquals(96, volume.getCornerTwo().getY()); // moving an existing corner shouldn't change its height
		assertEquals(64, volume.getCornerTwo().getZ());
		assertEquals(4, volume.getCornerTwo().getTypeId());
		assertEquals((byte)4, volume.getCornerTwo().getData());
	}

	@Test
	public void setNorthwest_whenChangingVolumeWithCorner1NeCorner2Sw_shouldMoveCorner1XAndCorner2Z() throws NotNorthwestException, TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(-64);	// further north
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(64);	// further west
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(-32, 32, -32, 2, (byte)2);	// corner 1 at minX and minZ (ne)
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		BlockInfo existingCorner2 = new BlockInfo(32, 96, 32, 4, (byte)4); // corner 2 at maxX and maxZ (sw)
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		volume.setNorthwest(blockMock);

		// Assert
		// first corner should move along x but not along y or z
		assertEquals(-64, volume.getCornerOne().getX());
		assertEquals(32, volume.getCornerOne().getY());
		assertEquals(-32, volume.getCornerOne().getZ());
		assertEquals(2, volume.getCornerOne().getTypeId());
		assertEquals((byte)2, volume.getCornerOne().getData());

		// second corner should move along z but not along x or y
		assertEquals(32, volume.getCornerTwo().getX());
		assertEquals(96, volume.getCornerTwo().getY());
		assertEquals(64, volume.getCornerTwo().getZ());
		assertEquals(4, volume.getCornerTwo().getTypeId());
		assertEquals((byte)4, volume.getCornerTwo().getData());
	}

	@Test
	public void setNorthwest_whenChangingVolumeWithCorner1SwCorner2Ne_shouldMoveCorner1ZAndCorner2X() throws NotNorthwestException, TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(-64);	// further north
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(64);	// further west
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(32, 32, 32, 2, (byte)2);	// corner 1 at maxX and maxZ (sw)
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		BlockInfo existingCorner2 = new BlockInfo(-32, 96, -32, 4, (byte)4); // corner 2 at minX and minZ (ne)
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		volume.setNorthwest(blockMock);

		// Assert
		// first corner should move along z but not along x or y
		assertEquals(32, volume.getCornerOne().getX());
		assertEquals(32, volume.getCornerOne().getY());
		assertEquals(64, volume.getCornerOne().getZ());
		assertEquals(2, volume.getCornerOne().getTypeId());
		assertEquals((byte)2, volume.getCornerOne().getData());

		// second corner should move along x but not along y or z
		assertEquals(-64, volume.getCornerTwo().getX());
		assertEquals(96, volume.getCornerTwo().getY());
		assertEquals(-32, volume.getCornerTwo().getZ());
		assertEquals(4, volume.getCornerTwo().getTypeId());
		assertEquals((byte)4, volume.getCornerTwo().getData());
	}

	// getNorthwestX

	// getNorthwestZ

	// setSoutheast

	@Test
	public void setSoutheast_whenCreatingAndNoCornersAreSet_shouldSetCorner2AtBottom() throws NotSoutheastException, TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(0);
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(0);
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		volume.setSoutheast(blockMock);

		// Assert
		assertEquals(null, volume.getCornerOne());
		assertEquals(0, volume.getCornerTwo().getX());
		assertEquals(0, volume.getCornerTwo().getY()); // the corner should shoot down
		assertEquals(0, volume.getCornerTwo().getZ());
		assertEquals(10, volume.getCornerTwo().getTypeId());
		assertEquals((byte)2, volume.getCornerTwo().getData());
	}

	@Test
	public void setSoutheast_whenCreatingAndNoCorner2IsSet_ButCorner1IsAlreadySet_AndNewCornerBlockIsToWestOfCorner1_shouldThrowNotSoutheastException() throws TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(64); // further south
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(64);	// further west
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		boolean failed = false;
		try {
			volume.setSoutheast(blockMock);
		}
		catch(NotSoutheastException e) {
			failed = true;
		}

		// Assert
		// first corner shouldn't move
		assertTrue(failed);
		assertEquals(null, volume.getCornerTwo());
		assertEquals(existingCorner1, volume.getCornerOne());
	}

	@Test
	public void setSoutheast_whenCreatingAndNoCorner2IsSet_ButCorner1IsAlreadySet_AndNewCornerBlockIsToNorthOfCorner1_shouldThrowNotSoutheastException() throws TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(-64); // further north
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(-64);	// further east
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		boolean failed = false;
		try {
			volume.setSoutheast(blockMock);
		}
		catch(NotSoutheastException e) {
			failed = true;
		}

		// Assert
		// first corner shouldn't move
		assertTrue(failed);
		assertEquals(null, volume.getCornerTwo());
		assertEquals(existingCorner1, volume.getCornerOne());
	}

	@Test
	public void setSoutheast_whenCreatingAndCorner2AlreadySet_shouldSetCorner1AtBottom() throws NotSoutheastException, TooSmallException, TooBigException{	// se always goes to bottom
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(64);  // further south
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(-64);  // further east
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner2 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		volume.setSoutheast(blockMock);

		// Assert
		// first corner shouldn't move
		assertEquals(0, volume.getCornerTwo().getX());
		assertEquals(64, volume.getCornerTwo().getY());
		assertEquals(0, volume.getCornerTwo().getZ());
		assertEquals(4, volume.getCornerTwo().getTypeId());
		assertEquals((byte)4, volume.getCornerTwo().getData());

		assertEquals(64, volume.getCornerOne().getX());
		assertEquals(0, volume.getCornerOne().getY()); // the new corner should shoot down
		assertEquals(-64, volume.getCornerOne().getZ());
		assertEquals(10, volume.getCornerOne().getTypeId());
		assertEquals((byte)2, volume.getCornerOne().getData());
	}

	@Test
	public void setSoutheast_whenCreating_AndCorner2AlreadySet_ButNewCornerBlockIsToWestOfCorner2_shouldThrowNotSoutheastException() throws TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(64); // further south
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(64);	// further west
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner2 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		boolean failed = false;
		try {
			volume.setSoutheast(blockMock);
		}
		catch(NotSoutheastException e) {
			failed = true;
		}

		// Assert
		// first corner shouldn't move
		assertTrue(failed);
		assertEquals(null, volume.getCornerOne());
		assertEquals(existingCorner2, volume.getCornerTwo());
	}

	@Test
	public void setSoutheast_whenCreating_AndCorner2AlreadySet_ButNewCornerBlockIsToNorthOfCorner2_shouldThrowNotSoutheastException() throws TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(-64); // further north
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(-64);	// further east
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner2 = new BlockInfo(0, 64, 0, 4, (byte)4);
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		boolean failed = false;
		try {
			volume.setSoutheast(blockMock);
		}
		catch(NotSoutheastException e) {
			failed = true;
		}

		// Assert
		// first corner shouldn't move
		assertTrue(failed);
		assertEquals(null, volume.getCornerOne());
		assertEquals(existingCorner2, volume.getCornerTwo());
	}

	@Test
	public void setSoutheast_whenChangingVolumeWithCorner1NwCorner2Se_shouldMoveCorner2() throws NotSoutheastException, TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(64);	// further south
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(-64);	// further east
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(-32, 32, 32, 2, (byte)2);	// corner 1 at minX and maxZ (nw)
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		BlockInfo existingCorner2 = new BlockInfo(32, 96, -32, 4, (byte)4); // corner 2 at maxX and minZ (se)
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		volume.setSoutheast(blockMock);

		// Assert
		// first corner shouldn't move
		assertEquals(-32, volume.getCornerOne().getX());
		assertEquals(32, volume.getCornerOne().getY());
		assertEquals(32, volume.getCornerOne().getZ());
		assertEquals(2, volume.getCornerOne().getTypeId());
		assertEquals((byte)2, volume.getCornerOne().getData());

		// second corner should move but not along y
		assertEquals(64, volume.getCornerTwo().getX());
		assertEquals(96, volume.getCornerTwo().getY());
		assertEquals(-64, volume.getCornerTwo().getZ());
		assertEquals(4, volume.getCornerTwo().getTypeId());
		assertEquals((byte)4, volume.getCornerTwo().getData());
	}

	@Test
	public void setSoutheast_whenChangingVolumeWithCorner1SeCorner2Nw_shouldMoveCorner1() throws NotSoutheastException, TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(64);	// further south
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(-64);	// further east
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(32, 32, -32, 2, (byte)2);	// corner 1 at maxX and minZ (se)
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		BlockInfo existingCorner2 = new BlockInfo(-32, 96, 32, 4, (byte)4); // corner 2 at minX and maxZ (nw)
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		volume.setSoutheast(blockMock);

		// Assert
		// first corner should move but not along y
		assertEquals(64, volume.getCornerOne().getX());
		assertEquals(32, volume.getCornerOne().getY());
		assertEquals(-64, volume.getCornerOne().getZ());
		assertEquals(2, volume.getCornerOne().getTypeId());
		assertEquals((byte)2, volume.getCornerOne().getData());

		// second corner shouldn't move
		assertEquals(-32, volume.getCornerTwo().getX());
		assertEquals(96, volume.getCornerTwo().getY());
		assertEquals(32, volume.getCornerTwo().getZ());
		assertEquals(4, volume.getCornerTwo().getTypeId());
		assertEquals((byte)4, volume.getCornerTwo().getData());
	}

	@Test
	public void setSoutheast_whenChangingVolumeWithCorner1NeCorner2Sw_shouldMoveCorner1ZAndCorner2X() throws NotSoutheastException, TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(64);	// further south
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(-64);	// further east
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(-32, 32, -32, 2, (byte)2);	// corner 1 at minX and minZ (ne)
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		BlockInfo existingCorner2 = new BlockInfo(32, 96, 32, 4, (byte)4); // corner 2 at maxX and maxZ (sw)
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		volume.setSoutheast(blockMock);

		// Assert
		// first corner should move along z but not along x or y
		assertEquals(-32, volume.getCornerOne().getX());
		assertEquals(32, volume.getCornerOne().getY());
		assertEquals(-64, volume.getCornerOne().getZ());
		assertEquals(2, volume.getCornerOne().getTypeId());
		assertEquals((byte)2, volume.getCornerOne().getData());

		// second corner should move along x but not along y or z
		assertEquals(64, volume.getCornerTwo().getX());
		assertEquals(96, volume.getCornerTwo().getY());
		assertEquals(32, volume.getCornerTwo().getZ());
		assertEquals(4, volume.getCornerTwo().getTypeId());
		assertEquals((byte)4, volume.getCornerTwo().getData());
	}

	@Test
	public void setSoutheast_whenChangingVolumeWithCorner1SwCorner2Ne_shouldMoveCorner1XAndCorner2Z() throws NotSoutheastException, TooSmallException, TooBigException{
		// Arrange
		War warMock = mock(War.class);
		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", warMock, worldMock, zoneMock);
		Block blockMock = mock(Block.class);
		when(blockMock.getX()).thenReturn(64);	// further south
		when(blockMock.getY()).thenReturn(64);	// at sea level
		when(blockMock.getZ()).thenReturn(-64);	// further east
		when(blockMock.getTypeId()).thenReturn(10);
		when(blockMock.getData()).thenReturn((byte)2);

		// Act
		BlockInfo existingCorner1 = new BlockInfo(32, 32, 32, 2, (byte)2);	// corner 1 at maxX and maxZ (sw)
		volume.setCornerOne(existingCorner1);	// corner 1 already set
		BlockInfo existingCorner2 = new BlockInfo(-32, 96, -32, 4, (byte)4); // corner 2 at minX and minZ (ne)
		volume.setCornerTwo(existingCorner2);	// corner 2 already set
		volume.setSoutheast(blockMock);

		// Assert
		// first corner should move along x but not along y or z
		assertEquals(64, volume.getCornerOne().getX());
		assertEquals(32, volume.getCornerOne().getY());
		assertEquals(32, volume.getCornerOne().getZ());
		assertEquals(2, volume.getCornerOne().getTypeId());
		assertEquals((byte)2, volume.getCornerOne().getData());

		// second corner should move along z but not along x or y
		assertEquals(-32, volume.getCornerTwo().getX());
		assertEquals(96, volume.getCornerTwo().getY());
		assertEquals(-64, volume.getCornerTwo().getZ());
		assertEquals(4, volume.getCornerTwo().getTypeId());
		assertEquals((byte)4, volume.getCornerTwo().getData());
	}

	// getSoutheastX

	// getSoutheastZ

	// setCornerOne

	// setCornerTwo

}
