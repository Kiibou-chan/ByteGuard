package space.kiibou.byteguard.agent

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import space.kiibou.byteguard.processor.info.ClassSpecificationInfo
import java.lang.instrument.Instrumentation

class PreMainAgent {

    companion object {

        @JvmStatic
        fun premain(agentArgs: String?, instrumentation: Instrumentation) {
            val specInfo: List<ClassSpecificationInfo> = getClassSpecInfo()

            println("SpecInfos: $specInfo")

            val transformer = ByteGuardClassFileTransformer(specInfo)

            instrumentation.addTransformer(transformer)

            println("Installed ByteGuardClassFileTransformer")
        }

        @OptIn(ExperimentalSerializationApi::class)
        fun getClassSpecInfo(): List<ClassSpecificationInfo> {
            val urls = this::class.java.classLoader.getResources("space/kiibou/byteguard/spec/ClassSpecs.json")

            return urls.toList().flatMap {
                Json.decodeFromStream<List<ClassSpecificationInfo>>(it.openStream())
            }
        }

    }

}
