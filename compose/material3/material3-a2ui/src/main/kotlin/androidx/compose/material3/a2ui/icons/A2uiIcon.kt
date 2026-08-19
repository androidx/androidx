/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.material3.a2ui.icons

import androidx.compose.material3.a2ui.icons.AccountCircle as AccountCircleIcon
import androidx.compose.material3.a2ui.icons.Add as AddIcon
import androidx.compose.material3.a2ui.icons.ArrowBack as ArrowBackIcon
import androidx.compose.material3.a2ui.icons.ArrowForward as ArrowForwardIcon
import androidx.compose.material3.a2ui.icons.AttachFile as AttachFileIcon
import androidx.compose.material3.a2ui.icons.CalendarToday as CalendarTodayIcon
import androidx.compose.material3.a2ui.icons.Call as CallIcon
import androidx.compose.material3.a2ui.icons.Camera as CameraIcon
import androidx.compose.material3.a2ui.icons.Check as CheckIcon
import androidx.compose.material3.a2ui.icons.Close as CloseIcon
import androidx.compose.material3.a2ui.icons.Delete as DeleteIcon
import androidx.compose.material3.a2ui.icons.Download as DownloadIcon
import androidx.compose.material3.a2ui.icons.Edit as EditIcon
import androidx.compose.material3.a2ui.icons.Error as ErrorIcon
import androidx.compose.material3.a2ui.icons.Event as EventIcon
import androidx.compose.material3.a2ui.icons.FastForward as FastForwardIcon
import androidx.compose.material3.a2ui.icons.FastRewind as FastRewindIcon
import androidx.compose.material3.a2ui.icons.Favorite as FavoriteIcon
import androidx.compose.material3.a2ui.icons.FavoriteOff as FavoriteOffIcon
import androidx.compose.material3.a2ui.icons.Folder as FolderIcon
import androidx.compose.material3.a2ui.icons.Help as HelpIcon
import androidx.compose.material3.a2ui.icons.Home as HomeIcon
import androidx.compose.material3.a2ui.icons.Info as InfoIcon
import androidx.compose.material3.a2ui.icons.LocationOn as LocationOnIcon
import androidx.compose.material3.a2ui.icons.Lock as LockIcon
import androidx.compose.material3.a2ui.icons.LockOpen as LockOpenIcon
import androidx.compose.material3.a2ui.icons.Mail as MailIcon
import androidx.compose.material3.a2ui.icons.Menu as MenuIcon
import androidx.compose.material3.a2ui.icons.MoreHoriz as MoreHorizIcon
import androidx.compose.material3.a2ui.icons.MoreVert as MoreVertIcon
import androidx.compose.material3.a2ui.icons.Notifications as NotificationsIcon
import androidx.compose.material3.a2ui.icons.NotificationsOff as NotificationsOffIcon
import androidx.compose.material3.a2ui.icons.Pause as PauseIcon
import androidx.compose.material3.a2ui.icons.Payment as PaymentIcon
import androidx.compose.material3.a2ui.icons.Person as PersonIcon
import androidx.compose.material3.a2ui.icons.Phone as PhoneIcon
import androidx.compose.material3.a2ui.icons.Photo as PhotoIcon
import androidx.compose.material3.a2ui.icons.PlayArrow as PlayArrowIcon
import androidx.compose.material3.a2ui.icons.Print as PrintIcon
import androidx.compose.material3.a2ui.icons.Refresh as RefreshIcon
import androidx.compose.material3.a2ui.icons.Search as SearchIcon
import androidx.compose.material3.a2ui.icons.Send as SendIcon
import androidx.compose.material3.a2ui.icons.Settings as SettingsIcon
import androidx.compose.material3.a2ui.icons.Share as ShareIcon
import androidx.compose.material3.a2ui.icons.ShoppingCart as ShoppingCartIcon
import androidx.compose.material3.a2ui.icons.SkipNext as SkipNextIcon
import androidx.compose.material3.a2ui.icons.SkipPrevious as SkipPreviousIcon
import androidx.compose.material3.a2ui.icons.Star as StarIcon
import androidx.compose.material3.a2ui.icons.StarHalf as StarHalfIcon
import androidx.compose.material3.a2ui.icons.StarOff as StarOffIcon
import androidx.compose.material3.a2ui.icons.Stop as StopIcon
import androidx.compose.material3.a2ui.icons.Upload as UploadIcon
import androidx.compose.material3.a2ui.icons.Visibility as VisibilityIcon
import androidx.compose.material3.a2ui.icons.VisibilityOff as VisibilityOffIcon
import androidx.compose.material3.a2ui.icons.VolumeDown as VolumeDownIcon
import androidx.compose.material3.a2ui.icons.VolumeMute as VolumeMuteIcon
import androidx.compose.material3.a2ui.icons.VolumeOff as VolumeOffIcon
import androidx.compose.material3.a2ui.icons.VolumeUp as VolumeUpIcon
import androidx.compose.material3.a2ui.icons.Warning as WarningIcon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/** Supported Material 3 icon names in the A2UI basic catalog schema. */
internal enum class A2uiIcon(val iconName: String, val icon: ImageVector) {
    AccountCircle("accountCircle", AccountCircleIcon),
    Add("add", AddIcon),
    ArrowBack("arrowBack", ArrowBackIcon),
    ArrowForward("arrowForward", ArrowForwardIcon),
    AttachFile("attachFile", AttachFileIcon),
    CalendarToday("calendarToday", CalendarTodayIcon),
    Call("call", CallIcon),
    Camera("camera", CameraIcon),
    Check("check", CheckIcon),
    Close("close", CloseIcon),
    Delete("delete", DeleteIcon),
    Download("download", DownloadIcon),
    Edit("edit", EditIcon),
    Event("event", EventIcon),
    Error("error", ErrorIcon),
    FastForward("fastForward", FastForwardIcon),
    Favorite("favorite", FavoriteIcon),
    FavoriteOff("favoriteOff", FavoriteOffIcon),
    Folder("folder", FolderIcon),
    Help("help", HelpIcon),
    Home("home", HomeIcon),
    Info("info", InfoIcon),
    LocationOn("locationOn", LocationOnIcon),
    Lock("lock", LockIcon),
    LockOpen("lockOpen", LockOpenIcon),
    Mail("mail", MailIcon),
    Menu("menu", MenuIcon),
    MoreVert("moreVert", MoreVertIcon),
    MoreHoriz("moreHoriz", MoreHorizIcon),
    NotificationsOff("notificationsOff", NotificationsOffIcon),
    Notifications("notifications", NotificationsIcon),
    Pause("pause", PauseIcon),
    Payment("payment", PaymentIcon),
    Person("person", PersonIcon),
    Phone("phone", PhoneIcon),
    Photo("photo", PhotoIcon),
    Play("play", PlayArrowIcon),
    Print("print", PrintIcon),
    Refresh("refresh", RefreshIcon),
    Rewind("rewind", FastRewindIcon),
    Search("search", SearchIcon),
    Send("send", SendIcon),
    Settings("settings", SettingsIcon),
    Share("share", ShareIcon),
    ShoppingCart("shoppingCart", ShoppingCartIcon),
    SkipNext("skipNext", SkipNextIcon),
    SkipPrevious("skipPrevious", SkipPreviousIcon),
    Star("star", StarIcon),
    StarHalf("starHalf", StarHalfIcon),
    StarOff("starOff", StarOffIcon),
    Stop("stop", StopIcon),
    Upload("upload", UploadIcon),
    Visibility("visibility", VisibilityIcon),
    VisibilityOff("visibilityOff", VisibilityOffIcon),
    VolumeDown("volumeDown", VolumeDownIcon),
    VolumeMute("volumeMute", VolumeMuteIcon),
    VolumeOff("volumeOff", VolumeOffIcon),
    VolumeUp("volumeUp", VolumeUpIcon),
    Warning("warning", WarningIcon);

    companion object {
        /** List of all supported icon name strings in the A2UI basic catalog schema. */
        val AllNames: List<String> = entries.fastMap { it.iconName }

        private val NameMap: Map<String, A2uiIcon> =
            buildMap(entries.size) {
                A2uiIcon.entries.fastForEach { icon -> put(icon.iconName, icon) }
            }

        /** Resolves an A2UI icon name to its corresponding [ImageVector]. */
        fun fromName(name: String?): ImageVector? = name?.let { NameMap[it]?.icon }
    }
}
