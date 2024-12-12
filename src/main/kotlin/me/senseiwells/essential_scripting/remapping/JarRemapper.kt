package me.senseiwells.essential_scripting.remapping

import joptsimple.OptionParser
import joptsimple.util.EnumConverter
import joptsimple.util.PathConverter
import joptsimple.util.PathProperties
import me.senseiwells.essential_scripting.script.configuration.MappingType
import me.senseiwells.essential_scripting.utils.ScriptRemappingUtils
import net.fabricmc.api.ModInitializer
import net.fabricmc.loader.api.FabricLoader
import kotlin.io.path.nameWithoutExtension
import kotlin.system.exitProcess

object JarRemapper: ModInitializer {
    override fun onInitialize() {
        val args = FabricLoader.getInstance().getLaunchArguments(true)
        val parser = OptionParser()
        val inputSpec = parser.accepts("remap-input-jar").withRequiredArg()
            .withValuesConvertedBy(PathConverter(PathProperties.FILE_EXISTING))
        val fromSpec = parser.accepts("remap-from").withRequiredArg()
            .withValuesConvertedBy(MappingTypeConverter)
        val toSpec = parser.accepts("remap-to").withRequiredArg()
            .withValuesConvertedBy(MappingTypeConverter)
        val outputSpec = parser.accepts("remap-output-jar").withRequiredArg()
            .withValuesConvertedBy(PathConverter())
        parser.allowsUnrecognizedOptions()

        val options = parser.parse(*args);
        val inputJar = options.valueOf(inputSpec) ?: return
        val from = options.valueOf(fromSpec)
        val to = options.valueOf(toSpec)
        if (from == null || to == null) {
            throw IllegalArgumentException(
                "Provided remap input jar but didn't specify what mappings from and what mappings to"
            )
        }

        val outputJar = options.valueOf(outputSpec)
            ?: inputJar.resolveSibling("${inputJar.nameWithoutExtension}-mapped-${to.id}.jar")

        ScriptRemappingUtils.remapJar(inputJar, outputJar, from, to)
        exitProcess(0)
    }

    private object MappingTypeConverter: EnumConverter<MappingType>(MappingType::class.java)
}
