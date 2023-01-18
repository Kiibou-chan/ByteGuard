package space.kiibou.jguard.agent

import space.kiibou.jguard.bytecode.BytecodeWeaver
import space.kiibou.jguard.processor.info.ClassSpecificationInfo
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

class JGuardClassFileTransformer(classSpecInfos: List<ClassSpecificationInfo>) : ClassFileTransformer {

    private val nameSpecInfoMap: Map<String, ClassSpecificationInfo> =
        classSpecInfos.associateBy { it.targetClassType.javaName.replace('.', '/') }

    override fun transform(
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain,
        classfileBuffer: ByteArray
    ): ByteArray? {
        if (!nameSpecInfoMap.containsKey(className)) return null

        println("JGuard: Transforming class $className")

        val specInfo = nameSpecInfoMap[className]!!

        val weaver = BytecodeWeaver(classfileBuffer, specInfo)

        return weaver.weave()
    }

}