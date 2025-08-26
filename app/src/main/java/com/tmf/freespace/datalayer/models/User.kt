import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tmf.freespace.datalayer.datasources.cloudstorage.CloudStorageType
import java.util.UUID

@Entity
data class User(
    val idGuid: UUID,
    val phoneNumber: String,
    val emailAddress: String,
    val password: String,
    var maxDiskSize: Int,
    var cloudStorageType: CloudStorageType,
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
)