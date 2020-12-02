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

package androidx.room.integration.testapp.vo;

import androidx.room.Embedded;
import androidx.room.Relation;

public class RobotAndHivemind {

    @Embedded
    public final Robot mRobot;

    @Relation(parentColumn = "mHiveId", entityColumn = "mId")
    public final Hivemind mHivemind;

    public RobotAndHivemind(Robot robot, Hivemind hivemind) {
        mRobot = robot;
        mHivemind = hivemind;
    }

    public Robot getRobot() {
        return mRobot;
    }

    public Hivemind getHivemind() {
        return mHivemind;
    }
}
