package com.example.carapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.carapp.R

enum class CarAppIcon(@get:DrawableRes val resource: Int) {
    CAR(R.drawable.ic_car), GARAGE(R.drawable.ic_garage), SERVICE(R.drawable.ic_service),
    COSTS(R.drawable.ic_wallet), CALENDAR(R.drawable.ic_calendar), MILEAGE(R.drawable.ic_gauge),
    EDIT(R.drawable.ic_edit), BACK(R.drawable.ic_back), MORE(R.drawable.ic_more), ADD(R.drawable.ic_plus),
    NEXT(R.drawable.ic_chevron), DROPDOWN(R.drawable.ic_arrow_drop_down)
}

@Composable
fun BackFormButton(text: String, onClick: () -> Unit, enabled: Boolean = true) {
    TextButton(onClick = onClick, enabled = enabled) {
        AppIcon(CarAppIcon.BACK, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text)
    }
}

@Composable
fun AppIcon(icon: CarAppIcon, description: String? = null, modifier: Modifier = Modifier,
            tint: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Icon(painterResource(icon.resource), description, modifier.size(24.dp), tint = tint)
}

@Composable
fun CarIllustration(modifier: Modifier = Modifier) {
    Image(painterResource(R.drawable.car_illustration), contentDescription = null, modifier = modifier)
}

@Composable
fun VehicleEmblem(modifier: Modifier = Modifier) {
    Icon(painterResource(R.drawable.ic_vehicle_front), contentDescription = null,
        modifier = modifier, tint = MaterialTheme.colorScheme.primary)
}
