//@Entity(
//    tableName = "plant_photos",
//    foreignKeys = [
//        ForeignKey(
//            entity = PlantEntity::class,
//            parentColumns = ["sku"],
//            childColumns = ["plantSku"],
//            onDelete = ForeignKey.CASCADE
//        )
//    ],
//    indices = [Index("plantSku")]
//)
//data class PhotoEntity(
//    @PrimaryKey val id: String = UUID.randomUUID().toString(),
//    val plantSku: String,
//    val url: String,
//    val timestamp: Long,
//    val note: String
//)