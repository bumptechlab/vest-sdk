package poetry.sdk.shf.http

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

const val MODE_NON = 0
const val MODE_HEADER = 1
const val MODE_COOKIE = 2
const val MODE_PATH = 3

@IntDef(value = [MODE_NON, MODE_HEADER, MODE_COOKIE, MODE_PATH])
@Retention(RetentionPolicy.SOURCE)
annotation class InterfaceStyleMode