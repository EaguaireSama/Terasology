package org.terasology.logic.systems;

import org.terasology.components.BlockComponent;
import org.terasology.components.BlockParticleEffectComponent;
import org.terasology.components.LocationComponent;
import org.terasology.entitySystem.EntityManager;
import org.terasology.entitySystem.EntityRef;
import org.terasology.entitySystem.componentSystem.EventHandlerSystem;
import org.terasology.entitySystem.ReceiveEvent;
import org.terasology.events.DamageEvent;
import org.terasology.events.FullHealthEvent;
import org.terasology.events.NoHealthEvent;
import org.terasology.game.CoreRegistry;
import org.terasology.logic.manager.AudioManager;
import org.terasology.logic.world.IWorldProvider;
import org.terasology.model.blocks.Block;
import org.terasology.model.blocks.management.BlockManager;
import org.terasology.rendering.physics.BulletPhysicsRenderer;

/**
 * @author Immortius <immortius@gmail.com>
 */
public class BlockEntitySystem implements EventHandlerSystem {
    private static byte EmptyBlockId = 0x0;

    private IWorldProvider worldProvider;
    private EntityManager entityManager;

    public void initialise() {
        entityManager = CoreRegistry.get(EntityManager.class);
        worldProvider = CoreRegistry.get(IWorldProvider.class);
    }

    @ReceiveEvent(components={BlockComponent.class})
    public void onDestroyed(NoHealthEvent event, EntityRef entity)
    {
        if (worldProvider == null) return;
        BlockComponent blockComp = entity.getComponent(BlockComponent.class);
        Block oldBlock = BlockManager.getInstance().getBlock(worldProvider.getBlock(blockComp.getPosition()));
        worldProvider.setBlock(blockComp.getPosition(), EmptyBlockId, true, true);

        // TODO: A bunch of notification?

        // Remove the upper block if it's a billboard
        byte upperBlockType = worldProvider.getBlock(blockComp.getPosition().x, blockComp.getPosition().y + 1, blockComp.getPosition().z);
        if (BlockManager.getInstance().getBlock(upperBlockType).getBlockForm() == Block.BLOCK_FORM.BILLBOARD) {
            worldProvider.setBlock(blockComp.getPosition().x, blockComp.getPosition().y + 1, blockComp.getPosition().z, (byte) 0x0, true, true);
            // TODO: Particles here too?
        }

        AudioManager.play("RemoveBlock", 0.6f);

        /* PHYSICS */
        CoreRegistry.get(BulletPhysicsRenderer.class).addLootableBlocks(blockComp.getPosition().toVector3f(), oldBlock);

        entity.destroy();
    }
    
    @ReceiveEvent(components={BlockComponent.class})
    public void onRepaired(FullHealthEvent event, EntityRef entity) {
        BlockComponent blockComp = entity.getComponent(BlockComponent.class);
        if (blockComp.temporary) {
            entity.destroy();
        }
    }
    
    @ReceiveEvent(components={BlockComponent.class})
    public void onDamaged(DamageEvent event, EntityRef entity) {
        BlockComponent blockComp = entity.getComponent(BlockComponent.class);

        EntityRef particlesEntity = entityManager.create();
        particlesEntity.addComponent(new LocationComponent(blockComp.getPosition().toVector3f()));

        BlockParticleEffectComponent particleEffect = new BlockParticleEffectComponent();
        particleEffect.spawnCount = 64;
        particleEffect.blockType = worldProvider.getBlock(blockComp.getPosition());
        particleEffect.initialVelocityRange.set(4, 4, 4);
        particleEffect.spawnRange.set(0.3f, 0.3f, 0.3f);
        particleEffect.destroyEntityOnCompletion = true;
        particleEffect.minSize = 0.05f;
        particleEffect.maxSize = 0.1f;
        particleEffect.minLifespan = 1f;
        particleEffect.maxLifespan = 1.5f;
        particleEffect.targetVelocity.set(0,-5, 0);
        particleEffect.acceleration.set(2f, 2f, 2f);
        particleEffect.collideWithBlocks = true;
        particlesEntity.addComponent(particleEffect);
    }

}
