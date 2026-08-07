package com.linkbridge.core.data
import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

@Entity(tableName="events") data class LinkEvent(@PrimaryKey(autoGenerate=true)val id:Long=0,val type:String,val detail:String,val at:Long=System.currentTimeMillis())
@Entity(tableName="transfers") data class TransferRecord(@PrimaryKey val id:String,val name:String,val size:Long,val direction:String,val status:String,val bytesDone:Long,val updatedAt:Long)
@Dao interface LinkDao { @Insert suspend fun add(e:LinkEvent); @Query("SELECT * FROM events ORDER BY at DESC LIMIT 200") fun events():Flow<List<LinkEvent>>; @Insert(onConflict=OnConflictStrategy.REPLACE)suspend fun save(t:TransferRecord); @Query("SELECT * FROM transfers ORDER BY updatedAt DESC")fun transfers():Flow<List<TransferRecord>> }
@Database(entities=[LinkEvent::class,TransferRecord::class],version=1,exportSchema=false) abstract class LinkDatabase:RoomDatabase(){abstract fun dao():LinkDao}
@Singleton class HistoryRepository @Inject constructor(db:LinkDatabase){private val dao=db.dao();fun events()=dao.events();fun transfers()=dao.transfers();suspend fun event(type:String,detail:String)=dao.add(LinkEvent(type=type,detail=detail));suspend fun transfer(x:TransferRecord)=dao.save(x)}
@Module @InstallIn(SingletonComponent::class) object DatabaseModule { @Provides @Singleton fun db(@ApplicationContext c:Context)=Room.databaseBuilder(c,LinkDatabase::class.java,"linkbridge.db").fallbackToDestructiveMigration().build() }
