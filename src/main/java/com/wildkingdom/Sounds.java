package com.wildkingdom;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Sounds {
    private Sounds() {}

    public static final DeferredRegister<SoundEvent> REG = DeferredRegister.create(Registries.SOUND_EVENT, WildKingdom.MODID);
    private static final Map<String, SoundEvent> EV = new HashMap<>();
    private static final String[] FAMS = {"roar", "growl", "trumpet", "bellow", "neigh", "squeak", "bark", "caw", "chirp", "screech", "hoot", "honk", "hiss", "click", "buzz", "whale", "whistle", "splash"};
    private static final Map<String, String> KIND = Map.ofEntries(
        Map.entry("tiger", "roar"),
        Map.entry("lion", "roar"),
        Map.entry("snow_leopard", "roar"),
        Map.entry("cheetah", "roar"),
        Map.entry("megalodon", "roar"),
        Map.entry("titanoboa", "roar"),
        Map.entry("scorpion_emperor", "roar"),
        Map.entry("grizzly", "growl"),
        Map.entry("hyena", "growl"),
        Map.entry("tasmanian_devil", "growl"),
        Map.entry("gorilla", "growl"),
        Map.entry("komodo", "growl"),
        Map.entry("cobra", "growl"),
        Map.entry("raccoon", "growl"),
        Map.entry("skunk", "growl"),
        Map.entry("elephant", "trumpet"),
        Map.entry("bison", "bellow"),
        Map.entry("hippo", "bellow"),
        Map.entry("moose", "bellow"),
        Map.entry("rhino", "bellow"),
        Map.entry("walrus", "bellow"),
        Map.entry("crocodile", "bellow"),
        Map.entry("caiman", "bellow"),
        Map.entry("giraffe", "bellow"),
        Map.entry("gazelle", "neigh"),
        Map.entry("zebra", "neigh"),
        Map.entry("kangaroo", "neigh"),
        Map.entry("emu", "neigh"),
        Map.entry("anteater", "neigh"),
        Map.entry("jerboa", "squeak"),
        Map.entry("capybara", "squeak"),
        Map.entry("red_panda", "squeak"),
        Map.entry("meerkat", "squeak"),
        Map.entry("lobster", "squeak"),
        Map.entry("crab", "squeak"),
        Map.entry("seal", "bark"),
        Map.entry("chameleon", "squeak"),
        Map.entry("crow", "caw"),
        Map.entry("seagull", "caw"),
        Map.entry("hummingbird", "chirp"),
        Map.entry("toucan", "chirp"),
        Map.entry("peacock", "chirp"),
        Map.entry("bald_eagle", "screech"),
        Map.entry("owl", "hoot"),
        Map.entry("penguin", "honk"),
        Map.entry("flamingo", "honk"),
        Map.entry("anaconda", "hiss"),
        Map.entry("rattlesnake", "hiss"),
        Map.entry("karapuzik", "click"),
        Map.entry("kozyavka", "click"),
        Map.entry("may_beetle", "click"),
        Map.entry("pretendyashka", "click"),
        Map.entry("cockroach", "click"),
        Map.entry("car_cockroach", "click"),
        Map.entry("telifon", "click"),
        Map.entry("strashila", "click"),
        Map.entry("tarantula", "click"),
        Map.entry("mantis", "click"),
        Map.entry("ant", "click"),
        Map.entry("fly", "buzz"),
        Map.entry("chironomid", "buzz"),
        Map.entry("fignya", "buzz"),
        Map.entry("dragonfly", "buzz"),
        Map.entry("sperm_whale", "whale"),
        Map.entry("orca", "whale"),
        Map.entry("narwhal", "whale"),
        Map.entry("hammerhead", "whistle"),
        Map.entry("manta_ray", "whistle"),
        Map.entry("catfish", "splash"),
        Map.entry("flying_fish", "splash"),
        Map.entry("moonfish", "splash"));

    static {
        for (String f : FAMS) {
            for (String r : new String[]{"ambient", "hurt", "death"}) {
                String n = f + "_" + r;
                REG.register(n, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(WildKingdom.MODID, n)));
            }
        }
    }

    /** returns {ambient, hurt, death} for a species id */
    public static SoundEvent[] get(String speciesId) {
        String f = KIND.getOrDefault(speciesId, "squeak");
        SoundEvent[] out = new SoundEvent[3];
        String[] roles = {"ambient", "hurt", "death"};
        for (int i = 0; i < 3; i++) {
            final String n = f + "_" + roles[i];
            out[i] = EV.computeIfAbsent(n, k -> REG.getEntries().stream()
                    .filter(h -> h.getId().getPath().equals(k)).findFirst().get().get());
        }
        return out;
    }
}
