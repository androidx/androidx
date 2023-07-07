/*
 * Copyright 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.room.integration.kotlintestapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.integration.kotlintestapp.vo.Cluster
import androidx.room.integration.kotlintestapp.vo.Hivemind
import androidx.room.integration.kotlintestapp.vo.Robot
import androidx.room.integration.kotlintestapp.vo.RobotAndHivemind
import java.util.UUID

@Dao
interface RobotsDao {
    @Insert
    fun putHivemind(hivemind: Hivemind)

    @Insert
    fun putRobot(robot: Robot)

    @Query("SELECT * FROM Hivemind")
    @Transaction
    fun getCluster(): List<Cluster>

    @Query("SELECT * FROM Robot WHERE mHiveId = :hiveId")
    fun getHiveRobots(hiveId: UUID?): List<Robot>

    @Query("SELECT * FROM Robot")
    @Transaction
    fun robotsWithHivemind(): List<RobotAndHivemind>
}
