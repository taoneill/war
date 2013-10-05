package com.tommytony.war.spec.volumes;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.World;
import org.junit.Test;

import com.tommytony.war.Team;
import com.tommytony.war.Warzone;
import com.tommytony.war.structure.Monument;
import com.tommytony.war.volume.NotNorthwestException;
import com.tommytony.war.volume.NotSoutheastException;
import com.tommytony.war.volume.TooBigException;
import com.tommytony.war.volume.TooSmallException;
import com.tommytony.war.volume.ZoneVolume;

public class ZoneVolumeSpecTest {

	// setNorthwest

	@Test
	public void setNorthwest_whenCreatingAndNoCornersAreSet_shouldSetCorner1AtTop()
			throws NotNorthwestException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		when(worldMock.getMaxHeight()).thenReturn(256);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, 0, 64, 0);

		// Act
		volume.setNorthwest(nw);

		// Assert
		Location movedOne = new Location(worldMock, 0, 256, 0);
		assertEquals(movedOne, volume.getCornerOne());
	}

	@Test(expected = NotNorthwestException.class)
	public void setNorthwest_whenCreating_AndNoCorner1IsSet_ButCorner2Set_AndNewCornerBlockIsToEastOfCorner2_shouldThrowNotNorthwestException()
			throws TooSmallException, TooBigException, NotNorthwestException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, -64, 64, -64);

		// Act
		Location existingCorner2 = new Location(worldMock, 0, 64, 0);
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setNorthwest(nw);
	}

	@Test(expected = NotNorthwestException.class)
	public void setNorthwest_whenCreating_AndNoCorner1IsSet_ButCorner2Set_AndNewCornerBlockIsToSouthOfCorner2_shouldThrowNotNorthwestException()
			throws TooSmallException, TooBigException, NotNorthwestException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, 64, 64, 64);

		// Act
		Location existingCorner2 = new Location(worldMock, 0, 64, 0);
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setNorthwest(nw);
	}

	@Test(expected = TooSmallException.class)
	public void setNorthwest_whenCreating_AndNoCorner1IsSet_ButCorner2Set_AndNewCornerBlockIsTooCloseToCorner2_shouldThrowTooSmallException()
			throws NotNorthwestException, TooBigException, TooSmallException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, -5, 64, 5);

		// Act
		Location existingCorner2 = new Location(worldMock, 0, 64, 0);
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setNorthwest(nw);
	}

	@Test(expected = TooBigException.class)
	public void setNorthwest_whenCreating_AndNoCorner1IsSet_ButCorner2Set_AndNewCornerBlockIsTooFarFromCorner2_shouldThrowTooBigException()
			throws NotNorthwestException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, -1000, 64, 1000);

		// Act
		Location existingCorner2 = new Location(worldMock, 0, 64, 0);
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setNorthwest(nw);
	}

	@Test
	public void setNorthwest_whenCreatingAndCorner1AlreadySet_shouldSetCorner2AtTop()
			throws NotNorthwestException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		when(worldMock.getMaxHeight()).thenReturn(256);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, -64, 64, 64);

		// Act
		Location existingCorner1 = new Location(worldMock, 0, 64, 0);
		volume.setCornerOne(existingCorner1); // corner 1 already set
		volume.setNorthwest(nw);

		// Assert
		// first corner shouldn't move
		assertEquals(existingCorner1, volume.getCornerOne());

		Location nwMax = new Location(worldMock, -64, 256, 64);
		assertEquals(nwMax, volume.getCornerTwo());
	}

	@Test(expected = NotNorthwestException.class)
	public void setNorthwest_whenCreating_AndCorner1AlreadySet_ButNewCornerBlockIsEastOfCorner1_shouldThrowNotNorthwestException()
			throws TooSmallException, TooBigException, NotNorthwestException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, -64, 64, -64);

		// Act
		Location existingCorner1 = new Location(worldMock, 0, 64, 0);
		volume.setCornerOne(existingCorner1); // corner 1 already set
		volume.setNorthwest(nw);
	}

	@Test(expected = NotNorthwestException.class)
	public void setNorthwest_whenCreating_AndCorner1AlreadySet_ButNewCornerBlockIsSouthOfCorner1_shouldThrowNotNorthwestException()
			throws TooSmallException, TooBigException, NotNorthwestException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, 64, 64, 64);

		// Act
		Location existingCorner1 = new Location(worldMock, 0, 64, 0);
		volume.setCornerOne(existingCorner1); // corner 1 already set
		volume.setNorthwest(nw);
	}

	@Test
	public void setNorthwest_whenChangingVolumeWithCorner1NwCorner2Se_shouldMoveCorner1()
			throws NotNorthwestException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, -64, 64, 64);

		// Act
		Location existingCorner1 = new Location(worldMock, -32, 32, 32); // nw
		volume.setCornerOne(existingCorner1); // corner 1 already set
		Location existingCorner2 = new Location(worldMock, 32, 96, -32); // se
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setNorthwest(nw);

		// Assert
		// first corner should move but not along y
		Location movedOne = new Location(worldMock, -64, 32, 64);
		assertEquals(movedOne, volume.getCornerOne());

		// second corner shouldn't move
		assertEquals(existingCorner2, volume.getCornerTwo());
	}

	@Test
	public void setNorthwest_whenChangingVolumeWithCorner1SeCorner2Nw_shouldMoveCorner2()
			throws NotNorthwestException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, -64, 64, 64);

		// Act
		Location existingCorner1 = new Location(worldMock, 32, 32, -32); // nw
		volume.setCornerOne(existingCorner1); // corner 1 already set
		Location existingCorner2 = new Location(worldMock, -32, 96, 32); // se
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setNorthwest(nw);

		// Assert
		// first corner shouldn't move
		assertEquals(existingCorner1, volume.getCornerOne());

		// second corner should move but not along y
		Location movedTwo = new Location(worldMock, -64, 96, 64);
		assertEquals(movedTwo, volume.getCornerTwo());
	}

	@Test
	public void setNorthwest_whenChangingVolumeWithCorner1NeCorner2Sw_shouldMoveCorner1XAndCorner2Z()
			throws NotNorthwestException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, -64, 64, 64);

		// Act
		Location existingCorner1 = new Location(worldMock, -32, 32, -32); // ne
		volume.setCornerOne(existingCorner1); // corner 1 already set
		Location existingCorner2 = new Location(worldMock, 32, 96, 32); // sw
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setNorthwest(nw);

		// Assert
		// first corner should move along x but not along y or z
		Location movedOne = new Location(worldMock, -64, 32, -32);
		assertEquals(movedOne, volume.getCornerOne());

		// second corner should move along z but not along x or y
		Location movedTwo = new Location(worldMock, 32, 96, 64);
		assertEquals(movedTwo, volume.getCornerTwo());
	}

	@Test
	public void setNorthwest_whenChangingVolumeWithCorner1SwCorner2Ne_shouldMoveCorner1ZAndCorner2X()
			throws NotNorthwestException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location nw = new Location(worldMock, -64, 64, 64);

		// Act
		Location existingCorner1 = new Location(worldMock, 32, 32, 32); // sw
		volume.setCornerOne(existingCorner1); // corner 1 already set
		Location existingCorner2 = new Location(worldMock, -32, 96, -32); // ne
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setNorthwest(nw);

		// Assert
		// first corner should move along z but not along x or y
		Location movedOne = new Location(worldMock, 32, 32, 64);
		assertEquals(movedOne, volume.getCornerOne());

		// second corner should move along x but not along y or z
		Location movedTwo = new Location(worldMock, -64, 96, -32);
		assertEquals(movedTwo, volume.getCornerTwo());
	}

	// getNorthwestX

	// getNorthwestZ

	// setSoutheast

	@Test
	public void setSoutheast_whenCreatingAndNoCornersAreSet_shouldSetCorner2AtBottom()
			throws NotSoutheastException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location se = new Location(worldMock, 0, 64, 0);

		// Act
		volume.setSoutheast(se);

		// Assert
		Location movedOne = new Location(worldMock, 0, 0, 0);
		assertEquals(movedOne, volume.getCornerTwo());
	}

	@Test(expected = NotSoutheastException.class)
	public void setSoutheast_whenCreatingAndNoCorner2IsSet_ButCorner1IsAlreadySet_AndNewCornerBlockIsToWestOfCorner1_shouldThrowNotSoutheastException()
			throws TooSmallException, TooBigException, NotSoutheastException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location se = new Location(worldMock, 64, 64, 64);

		// Act
		Location existingCorner1 = new Location(worldMock, 0, 64, 0);
		volume.setCornerOne(existingCorner1); // corner 1 already set
		volume.setSoutheast(se);
	}

	@Test(expected = NotSoutheastException.class)
	public void setSoutheast_whenCreatingAndNoCorner2IsSet_ButCorner1IsAlreadySet_AndNewCornerBlockIsToNorthOfCorner1_shouldThrowNotSoutheastException()
			throws TooSmallException, TooBigException, NotSoutheastException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location se = new Location(worldMock, -64, 64, -64);

		// Act
		Location existingCorner1 = new Location(worldMock, 0, 64, 0);
		volume.setCornerOne(existingCorner1); // corner 1 already set
		volume.setSoutheast(se);
	}

	@Test
	public void setSoutheast_whenCreatingAndCorner2AlreadySet_shouldSetCorner1AtBottom()
			throws NotSoutheastException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location se = new Location(worldMock, 64, 64, -64);

		// Act
		Location existingCorner2 = new Location(worldMock, 0, 64, 0);
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setSoutheast(se);

		// Assert
		// first corner shouldn't move
		assertEquals(existingCorner2, volume.getCornerTwo());

		Location movedSe = new Location(worldMock, 64, 0, -64);
		assertEquals(movedSe, volume.getCornerOne());
	}

	@Test(expected = NotSoutheastException.class)
	public void setSoutheast_whenCreating_AndCorner2AlreadySet_ButNewCornerBlockIsToWestOfCorner2_shouldThrowNotSoutheastException()
			throws TooSmallException, TooBigException, NotSoutheastException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location se = new Location(worldMock, 64, 64, 64);

		// Act
		Location existingCorner2 = new Location(worldMock, 0, 64, 0);
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setSoutheast(se);
	}

	@Test(expected = NotSoutheastException.class)
	public void setSoutheast_whenCreating_AndCorner2AlreadySet_ButNewCornerBlockIsToNorthOfCorner2_shouldThrowNotSoutheastException()
			throws TooSmallException, TooBigException, NotSoutheastException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location se = new Location(worldMock, -64, 64, -64);

		// Act
		Location existingCorner2 = new Location(worldMock, 0, 64, 0);
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setSoutheast(se);
	}

	@Test
	public void setSoutheast_whenChangingVolumeWithCorner1NwCorner2Se_shouldMoveCorner2()
			throws NotSoutheastException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location se = new Location(worldMock, 64, 64, -64);

		// Act
		Location existingCorner1 = new Location(worldMock, -32, 32, 32); // nw
		volume.setCornerOne(existingCorner1); // corner 1 already set
		Location existingCorner2 = new Location(worldMock, 32, 96, -32); // se
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setSoutheast(se);

		// Assert
		// first corner shouldn't move
		assertEquals(existingCorner1, volume.getCornerOne());

		// second corner should move but not along y
		Location movedTwo = new Location(worldMock, 64, 96, -64);
		assertEquals(movedTwo, volume.getCornerTwo());
	}

	@Test
	public void setSoutheast_whenChangingVolumeWithCorner1SeCorner2Nw_shouldMoveCorner1()
			throws NotSoutheastException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location se = new Location(worldMock, 64, 64, -64);

		// Act
		Location existingCorner1 = new Location(worldMock, 32, 32, -32); // se
		volume.setCornerOne(existingCorner1); // corner 1 already set
		Location existingCorner2 = new Location(worldMock, -32, 96, 32); // nw
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setSoutheast(se);

		// Assert
		// first corner should move but not along y
		Location movedOne = new Location(worldMock, 64, 32, -64);
		assertEquals(movedOne, volume.getCornerOne());

		// second corner shouldn't move
		assertEquals(existingCorner2, volume.getCornerTwo());
	}

	@Test
	public void setSoutheast_whenChangingVolumeWithCorner1NeCorner2Sw_shouldMoveCorner1ZAndCorner2X()
			throws NotSoutheastException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location se = new Location(worldMock, 64, 64, -64);

		// Act
		Location existingCorner1 = new Location(worldMock, -32, 32, -32); // ne
		volume.setCornerOne(existingCorner1); // corner 1 already set
		Location existingCorner2 = new Location(worldMock, 32, 96, 32); // sw
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setSoutheast(se);

		// Assert
		// first corner should move along z but not along x or y
		Location movedOne = new Location(worldMock, -32, 32, -64);
		assertEquals(movedOne, volume.getCornerOne());

		// second corner should move along x but not along y or z
		Location movedTwo = new Location(worldMock, 64, 96, 32);
		assertEquals(movedTwo, volume.getCornerTwo());
	}

	@Test
	public void setSoutheast_whenChangingVolumeWithCorner1SwCorner2Ne_shouldMoveCorner1XAndCorner2Z()
			throws NotSoutheastException, TooSmallException, TooBigException {
		// Arrange

		World worldMock = mock(World.class);
		Warzone zoneMock = mock(Warzone.class);
		when(zoneMock.getTeams()).thenReturn(new ArrayList<Team>());
		when(zoneMock.getMonuments()).thenReturn(new ArrayList<Monument>());
		ZoneVolume volume = new ZoneVolume("test", worldMock, zoneMock);
		Location se = new Location(worldMock, 64, 64, -64);

		// Act
		Location existingCorner1 = new Location(worldMock, 32, 32, 32); // sw
		volume.setCornerOne(existingCorner1); // corner 1 already set
		Location existingCorner2 = new Location(worldMock, -32, 96, -32); // ne
		volume.setCornerTwo(existingCorner2); // corner 2 already set
		volume.setSoutheast(se);

		// Assert
		// first corner should move along x but not along y or z
		Location movedOne = new Location(worldMock, 64, 32, 32);
		assertEquals(movedOne, volume.getCornerOne());

		// second corner should move along z but not along x or y
		Location movedTwo = new Location(worldMock, -32, 96, -64);
		assertEquals(movedTwo, volume.getCornerTwo());
	}

	// getSoutheastX

	// getSoutheastZ

	// setCornerOne

	// setCornerTwo

}
