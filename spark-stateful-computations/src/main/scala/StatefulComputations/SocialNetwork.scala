package StatefulComputations

import org.apache.spark.sql.streaming.{GroupState, GroupStateTimeout}
import org.apache.spark.sql.{Dataset, SparkSession}

/**
 * - arbitrary social network that stores everything
 * - compute the average storage used by every post type
 */

object SocialNetwork {

	val spark: SparkSession = SparkSession.builder()
		.appName("SC")
		.master("local[2]")
		.getOrCreate()

	spark.sparkContext.setLogLevel("WARN")

	import spark.implicits._

	case class SocialPostRecord(postType: String, count: Int, storageUsed: Int)

	case class SocialPostBulk(postType: String, totalCount: Int, totalStorageUsed: Int)

	case class AveragePostStorage(postType: String, averageStorage: Double)

	/**
	 * [sample-data]
	 *
	 * -- batch 1
	 * text,3,3000<br />
	 * text,4,5000<br />
	 * video,1,500000<br />
	 * audio,3,60000<br />
	 *
	 * -- batch 2 <br />
	 * text,1,2500 <br />
	 */

	def readSocialUpdates(): Dataset[SocialPostRecord] = spark.readStream
		.format("socket")
		.option("host", "localhost")
		.option("port", 12345)
		.load()
		.as[String]
		.map { line =>
			val tokens = line.split(",")
			SocialPostRecord(tokens(0), tokens(1).toInt, tokens(2).toInt)
		}

	def updateAverageStorage(
		                        postType: String, // the key by which the grouping was made
		                        group: Iterator[SocialPostRecord], // [data-from-stream] => a [batch] of data associated with the key
		                        state: GroupState[SocialPostBulk] // [aggregate-data] => like an "option", managed manually
	                        ): AveragePostStorage // a  single value to output for entire batch
	= {
		/**
		 * [process]:<br />
		 * - extract state to start with [previousBulk]<br />
		 * - for all items in the group<br />
		 * - |  aggregate data:<br />
		 * - |      summing up total count<br />
		 * - |      summing up total storage<br />
		 * - | calculate average<br />
		 * - update state with the new aggregated data<br />
		 * - return a new single value of type [AveragePostStorage]<br />
		 */

		val previousBulk =
			if (state.exists) state.get
			else SocialPostBulk(postType, 0, 0)

		val (totalCount, totalStorage) = group.foldLeft((0, 0)) { (currentData: (Int, Int), record: SocialPostRecord) =>
			val (currentCount, currentStorage) = currentData
			(currentCount + record.count, currentStorage + record.storageUsed)
		}

		val newPostBulk = SocialPostBulk(postType, totalCount = previousBulk.totalCount + totalCount, totalStorageUsed = previousBulk.totalStorageUsed + totalStorage)
		state.update(newPostBulk)

		AveragePostStorage(postType, averageStorage = newPostBulk.totalStorageUsed * 1.0 / newPostBulk.totalCount)

	}

	def averagePostStorage(): Unit = {
		val socialStream = readSocialUpdates()

		val avgByPostType = socialStream
			.groupByKey(_.postType) // => KeyValueGroupedDataset[String, SocialPostRecord]
			.mapGroupsWithState(GroupStateTimeout.NoTimeout())(updateAverageStorage)

		avgByPostType.writeStream
			.format("console")
			.outputMode("update") // append mode not supported on mapGroupsWithState
			.start()
			.awaitTermination()

	}


	def main(args: Array[String]): Unit = {
		averagePostStorage()
	}

}
