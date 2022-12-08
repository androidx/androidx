/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials.playservices.createkeycredential

class CreatePublicKeyCredentialControllerUtils {
    companion object {

         // all required and optional types are here
         const val CREATE_REQUEST_INPUT_REQUIRED_AND_OPTIONAL = ("{\"rp\": {\"name\": " +
             "\"Address Book\", " + "\"id\": " +
             "\"addressbook-c7876.uc.r.appspot.com\"}, \"user\": {\"id\": " +
             "\"QjFpVTZDbENOVlU2NXBCd3ZCejlwc0Fqa0ZjMg\", \"name\": \"lee@gmail.com\", " +
             "\"displayName\": \"lee@gmail.com\", \"icon\": \"\"}, \"challenge\": " +
             "\"RkKbM6yyNpuM-_46Gdb49xxi09fH6zD267vuXEzTM2WrfTSfPL" +
             "-6gEAHY_HHPaQKh0ANgge2p1j0Mb7xOTKFBQ\", \"pubKeyCredParams\": [{\"type\": " +
             "\"public-key\", \"alg\": -7}, {\"type\": " +
             "\"public-key\", \"alg\": -36}, {\"type\": \"public-key\", \"alg\": -37}, " +
             "{\"type\": \"public-key\", \"alg\": -38}, {\"type\": \"public-key\", \"alg\": " +
             "-39}, {\"type\": \"public-key\", \"alg\": -257}, {\"type\": \"public-key\", " +
             "\"alg\": -258}, {\"type\": \"public-key\", \"alg\": -259}], \"timeout\": 60000, " +
             "\"excludeCredentials\": [{\"id\":\"AA\",\"type\":\"A\",\"transports\"=[\"A\"]}], " +
             "\"authenticatorSelection\": " +
             "{\"authenticatorAttachment\": \"platform\", \"residentKey\": \"required\", " +
             "\"requireResidentKey\": true, \"userVerification\": \"preferred\"}, " +
             "\"attestation\": \"none\"}")

         const val CREATE_REQUEST_INPUT_REQUIRED_ONLY = ("{\"rp\": {\"name\": " + "\"Address " +
             "Book\", " + "\"id\": " + "\"addressbook-c7876.uc.r.appspot.com\"}, " +
             "\"user\": {\"id\": " +
             "\"QjFpVTZDbENOVlU2NXBCd3ZCejlwc0Fqa0ZjMg\", \"name\": \"lee@gmail.com\", " +
             "\"displayName\": \"lee@gmail.com\", \"icon\": \"\"}, \"challenge\": " +
             "\"RkKbM6yyNpuM-_46Gdb49xxi09fH6zD267vuXEzTM2WrfTSfPL" +
             "-6gEAHY_HHPaQKh0ANgge2p1j0Mb7xOTKFBQ\", \"pubKeyCredParams\": [{\"type\": " +
             "\"public-key\", \"alg\": -7}, {\"type\": " +
             "\"public-key\", \"alg\": -36}, {\"type\": \"public-key\", \"alg\": -37}, " +
             "{\"type\": \"public-key\", \"alg\": -38}, {\"type\": \"public-key\", \"alg\": " +
             "-39}, {\"type\": \"public-key\", \"alg\": -257}, {\"type\": \"public-key\", " +
             "\"alg\": -258}, {\"type\": \"public-key\", \"alg\": -259}]," +
             "\"excludeCredentials\": []," + "\"attestation\": \"none\"}")
    }
}