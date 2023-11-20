package be.minelabs.advancement.criterion;

import be.minelabs.Minelabs;
import be.minelabs.advancement.NeighbourhoodCriterion;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraft.advancement.criterion.AbstractCriterionConditions;
import net.minecraft.predicate.entity.AdvancementEntityPredicateDeserializer;
import net.minecraft.predicate.entity.AdvancementEntityPredicateSerializer;
import net.minecraft.predicate.entity.EntityPredicate;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class CoulombCriterion extends NeighbourhoodCriterion<CoulombCriterion.Condition> {

    public enum Type {
        MOVE,
        ANNIHILATE,
        DECAY
    }

    public static final Identifier IDENTIFIER = new Identifier(Minelabs.MOD_ID, "coulomb_observe");

    @Override
    protected Condition conditionsFromJson(JsonObject obj, EntityPredicate.Extended playerPredicate, AdvancementEntityPredicateDeserializer predicateDeserializer) {
        String typeStr = obj.getAsJsonPrimitive("type").getAsString();
        if (typeStr == null)
            throw new JsonParseException("Missing type for CoulombForceCriterion");
        typeStr = typeStr.toUpperCase();
        try {
            Type type = Type.valueOf(typeStr);
            return new Condition(playerPredicate, type);
        } catch (IllegalArgumentException e) {
            throw new JsonParseException("Invalid type found: " + typeStr);
        }
    }

    @Override
    public Identifier getId() {
        return IDENTIFIER;
    }

    public void trigger(ServerPlayerEntity player, Type type) {
        trigger(player, condition -> condition.test(type));
    }

    public static class Condition extends AbstractCriterionConditions {

        private final Type type;

        private Condition(EntityPredicate.Extended playerPredicate, Type type) {
            super(IDENTIFIER, playerPredicate);
            this.type = type;
        }

        public boolean test(Type observedType){
            return this.type == observedType;
        }

        @Override
        public JsonObject toJson(AdvancementEntityPredicateSerializer predicateSerializer) {
            JsonObject jsonObject = super.toJson(predicateSerializer);
            jsonObject.add("type", new JsonPrimitive(type.toString()));
            return jsonObject;
        }
    }

}
