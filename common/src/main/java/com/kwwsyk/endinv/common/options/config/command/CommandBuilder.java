package com.kwwsyk.endinv.common.options.config.command;

import com.kwwsyk.endinv.common.options.config.ComplexConfigEntryImpl;
import com.kwwsyk.endinv.common.options.config.ConfigEntryImpl;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public final class CommandBuilder {
    private CommandBuilder() {}

    public static ArgumentBuilder<CommandSourceStack, ?> buildNode(ConfigEntryImpl<?> entry) {
        return switch (entry) {
            case ConfigEntryImpl.BooleanEntry value -> booleanNode(value);
            case ConfigEntryImpl.IntEntry value -> intNode(value);
            case ConfigEntryImpl.LongEntry value -> longNode(value);
            case ConfigEntryImpl.FloatEntry value -> floatNode(value);
            case ConfigEntryImpl.DoubleEntry value -> doubleNode(value);
            case ConfigEntryImpl.StringEntry value -> stringNode(value);
            case ConfigEntryImpl.EnumEntry<?> value -> enumNode(value);
            case ConfigEntryImpl.ListEntry<?> value -> listNode(value);
            case ComplexConfigEntryImpl<?> complex -> {
                LiteralArgumentBuilder<CommandSourceStack> parent = Commands.literal(complex.key())
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(() -> Component.literal(String.join("\n", complex.print())), false);
                            return 1;
                        });
                for (ConfigEntryImpl<?> field : complex.fields()) parent.then(buildNode(field));
                yield parent;
            }
        };
    }

    private static LiteralArgumentBuilder<CommandSourceStack> booleanNode(ConfigEntryImpl.BooleanEntry entry) {
        return leaf(entry).then(Commands.argument(entry.key(), BoolArgumentType.bool()).executes(ctx -> {
            boolean value = BoolArgumentType.getBool(ctx, entry.key());
            entry.set(value);
            return changed(ctx.getSource(), entry.key(), value);
        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> intNode(ConfigEntryImpl.IntEntry entry) {
        return leaf(entry).then(Commands.argument(entry.key(), IntegerArgumentType.integer(entry.getMin(), entry.getMax())).executes(ctx -> {
            int value = IntegerArgumentType.getInteger(ctx, entry.key()); entry.set(value);
            return changed(ctx.getSource(), entry.key(), value);
        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> longNode(ConfigEntryImpl.LongEntry entry) {
        return leaf(entry).then(Commands.argument(entry.key(), LongArgumentType.longArg(entry.getMin(), entry.getMax())).executes(ctx -> {
            long value = LongArgumentType.getLong(ctx, entry.key()); entry.set(value);
            return changed(ctx.getSource(), entry.key(), value);
        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> floatNode(ConfigEntryImpl.FloatEntry entry) {
        return leaf(entry).then(Commands.argument(entry.key(), FloatArgumentType.floatArg(entry.getMin(), entry.getMax())).executes(ctx -> {
            float value = FloatArgumentType.getFloat(ctx, entry.key()); entry.set(value);
            return changed(ctx.getSource(), entry.key(), value);
        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> doubleNode(ConfigEntryImpl.DoubleEntry entry) {
        return leaf(entry).then(Commands.argument(entry.key(), DoubleArgumentType.doubleArg(entry.getMin(), entry.getMax())).executes(ctx -> {
            double value = DoubleArgumentType.getDouble(ctx, entry.key()); entry.set(value);
            return changed(ctx.getSource(), entry.key(), value);
        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> stringNode(ConfigEntryImpl.StringEntry entry) {
        return leaf(entry).then(Commands.argument(entry.key(), StringArgumentType.greedyString()).executes(ctx -> {
            String value = StringArgumentType.getString(ctx, entry.key()); entry.set(value);
            return changed(ctx.getSource(), entry.key(), value);
        }));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static LiteralArgumentBuilder<CommandSourceStack> enumNode(ConfigEntryImpl.EnumEntry<?> entry) {
        Class enumClass = entry.defaultValue().getClass();
        return leaf(entry).then(Commands.argument(entry.key(), StringArgumentType.word())
                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                        Arrays.stream((Enum[]) enumClass.getEnumConstants()).map(Enum::name), builder))
                .executes(ctx -> {
                    String name = StringArgumentType.getString(ctx, entry.key());
                    try {
                        Enum<?> value;
                        try { value = Enum.valueOf(enumClass, name); }
                        catch (IllegalArgumentException ex) { value = Enum.valueOf(enumClass, name.toUpperCase()); }
                        ((ConfigEntryImpl.EnumEntry) entry).set(value);
                        return changed(ctx.getSource(), entry.key(), value.name());
                    } catch (IllegalArgumentException ex) {
                        ctx.getSource().sendFailure(Component.literal("Invalid enum value: " + name));
                        return 0;
                    }
                }));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static LiteralArgumentBuilder<CommandSourceStack> listNode(ConfigEntryImpl.ListEntry<?> entry) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal(entry.key()).executes(ctx -> {
            ctx.getSource().sendSuccess(() -> Component.literal(entry.key() + " = " + entry.get()), false);
            return 1;
        });
        node.then(Commands.literal("add").then(Commands.argument("value", StringArgumentType.greedyString()).executes(ctx -> {
            List current = entry.get();
            if (current.size() >= entry.getMaxLen()) return 0;
            Object value = parse(StringArgumentType.getString(ctx, "value"), elementClass(entry));
            if (!entry.getNewValPredicate().test(value)) return 0;
            var copy = new java.util.ArrayList(current); copy.add(value); ((ConfigEntryImpl.ListEntry) entry).set(copy);
            return changed(ctx.getSource(), entry.key(), copy);
        })));
        node.then(Commands.literal("remove").then(Commands.argument("index", IntegerArgumentType.integer(0)).executes(ctx -> {
            List current = entry.get(); int index = IntegerArgumentType.getInteger(ctx, "index");
            if (index >= current.size() || current.size() <= entry.getMinLen()) return 0;
            var copy = new java.util.ArrayList(current); copy.remove(index); ((ConfigEntryImpl.ListEntry) entry).set(copy);
            return changed(ctx.getSource(), entry.key(), copy);
        })));
        return node;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> leaf(ConfigEntryImpl<?> entry) {
        return Commands.literal(entry.key()).executes(ctx -> {
            ctx.getSource().sendSuccess(() -> Component.literal(entry.key() + " = " + entry.get()), false);
            return 1;
        });
    }

    private static int changed(CommandSourceStack source, String key, Object value) {
        source.sendSuccess(() -> Component.literal("Set " + key + " = " + value), true);
        return 1;
    }

    private static Class<?> elementClass(ConfigEntryImpl.ListEntry<?> entry) {
        return !entry.defaultValue().isEmpty() && entry.defaultValue().getFirst() != null
                ? entry.defaultValue().getFirst().getClass() : String.class;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object parse(String raw, Class<?> type) {
        try {
            if (type == Integer.class) return Integer.parseInt(raw);
            if (type == Long.class) return Long.parseLong(raw);
            if (type == Float.class) return Float.parseFloat(raw);
            if (type == Double.class) return Double.parseDouble(raw);
            if (type == Boolean.class) return Boolean.parseBoolean(raw);
            if (type.isEnum()) return Enum.valueOf((Class) type, raw.toUpperCase());
        } catch (RuntimeException ignored) {}
        return raw;
    }
}
