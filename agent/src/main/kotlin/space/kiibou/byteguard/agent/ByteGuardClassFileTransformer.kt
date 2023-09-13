package space.kiibou.byteguard.agent

import space.kiibou.byteguard.bytecode.BytecodeWeaver
import space.kiibou.byteguard.processor.info.ClassSpecificationInfo
import java.io.IOException
import java.lang.instrument.ClassFileTransformer
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.security.ProtectionDomain
import kotlin.io.path.Path

class ByteGuardClassFileTransformer(classSpecInfos: List<ClassSpecificationInfo>) : ClassFileTransformer {

    private val nameSpecInfoMap: Map<String, ClassSpecificationInfo> =
        classSpecInfos.associateBy { it.targetClassType.javaName.replace('.', '/') }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray? {
        try {
            if (!nameSpecInfoMap.containsKey(className)) return null

            println("ByteGuard: Transforming class $className")

            val specInfo = nameSpecInfoMap[className]!!

            val weaver = BytecodeWeaver(classfileBuffer, specInfo)

            val newBytecode = weaver.weave()

            try {
                val path = Path("build/bytecode/$className.class")

                path.parent.toFile().mkdirs()

                Files.write(
                    path,
                    newBytecode,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE
                )
            } catch (ex: IOException) {
                ex.printStackTrace()
            }

            println("ByteGuard: Finished transforming class $className")

            return newBytecode
        } catch (th: Throwable) {
            th.printStackTrace(System.err)
            return null
        }
    }

}
