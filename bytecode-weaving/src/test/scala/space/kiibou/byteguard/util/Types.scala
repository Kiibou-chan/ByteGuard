package space.kiibou.byteguard.util

import space.kiibou.byteguard.processor.info.ClassifierType

object Types {

    val ObjectType = new ClassifierType("java/lang/Object")
    val StringType = new ClassifierType("java/lang/String")

    val DoubleType = new ClassifierType("java/lang/Double")
    val IntegerType = new ClassifierType("java/lang/Integer")

}
