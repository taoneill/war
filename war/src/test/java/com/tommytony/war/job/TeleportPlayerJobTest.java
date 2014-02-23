package com.tommytony.war.job;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TeleportPlayerJobTest {
	World world;
	Player player;
	TeleportPlayerJob job;

	@Before
	public void setUp() throws Exception {
		player = mock(Player.class);
		world = mock(World.class);
		when(world.getName()).thenReturn("world");
		when(player.getHealth()).thenReturn(20.0);
		when(player.getLocation()).thenReturn(new Location(world, 0, 0, 0));
		job = new TeleportPlayerJob(player, new Location(world, 0, 0, 0));
	}

	@Test
	public void testPlayerMove_noMovement_false() {
		when(player.getLocation()).thenReturn(new Location(world, 0, 0, 0));

		assertFalse(job.hasPlayerMoved());
	}

	@Test
	public void testPlayerMove_smallMovement_false() {
		when(player.getLocation()).thenReturn(new Location(world, 0.5, 0.5, 0.5));

		assertFalse(job.hasPlayerMoved());
	}

	@Test
	public void testPlayerMove_largeMovement_true() {
		when(player.getLocation()).thenReturn(new Location(world, 5, 1, 3));

		assertTrue(job.hasPlayerMoved());
	}

	@Test
	public void testPlayerDamage_noDamage_false() {
		when(player.getHealth()).thenReturn(20.0);

		assertFalse(job.hasPlayerTakenDamage());
	}

	@Test
	public void testPlayerDamage_slightDamage_false() {
		when(player.getHealth()).thenReturn(19.5);

		assertFalse(job.hasPlayerTakenDamage());
	}

	@Test
	public void testPlayerDamage_largeDamage_true() {
		when(player.getHealth()).thenReturn(14.0);

		assertTrue(job.hasPlayerTakenDamage());
	}
}
