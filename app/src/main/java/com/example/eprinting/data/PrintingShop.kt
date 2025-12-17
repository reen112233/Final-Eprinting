import com.example.eprinting.data.PaperOption

data class Shop(
    val id: String,
    val shopName: String,
    val shopLocation: String,
    val ownerId: String,
    val paperOptions: List<PaperOption> = emptyList() // added
)