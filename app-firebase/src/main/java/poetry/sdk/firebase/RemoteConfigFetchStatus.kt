package poetry.sdk.firebase

import androidx.annotation.StringDef


@StringDef(RC_FETCH_STATUS_UNDECIDED, RC_FETCH_STATUS_SUCCEEDED, RC_FETCH_STATUS_FAILED)
@Retention(AnnotationRetention.SOURCE)
annotation class RemoteConfigFetchStatus


const val RC_FETCH_STATUS_UNDECIDED = "undecided"
const val RC_FETCH_STATUS_SUCCEEDED = "succeeded"
const val RC_FETCH_STATUS_FAILED = "failed"