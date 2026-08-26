const fs = require('fs');
const data = fs.readFileSync('app/src/main/java/com/pixelwater/app/ui/AppearanceSettingsSection.kt', 'utf8');

const anchor = '    // 1. THEME & COLORS\n    item {';
const replacement = `    // 0. LIVE PREVIEW
    item {
        val generalCornerRadius by viewModel.generalCornerRadius.collectAsStateWithLifecycle()
        val navbarCornerRadius by viewModel.navbarCornerRadius.collectAsStateWithLifecycle()
        val showOutlines by viewModel.showOutlines.collectAsStateWithLifecycle()
        
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (appLanguage == "el") "ΠΡΟΕΠΙΣΚΟΠΗΣΗ" else "LIVE PREVIEW",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 16.dp, bottom = 4.dp)
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Simulated mini-homepage
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mock Cards
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Card(
                            modifier = Modifier.weight(1f).height(100.dp),
                            shape = RoundedCornerShape(maxOf(0, generalCornerRadius).dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Card(
                            modifier = Modifier.weight(1f).height(100.dp),
                            shape = RoundedCornerShape(maxOf(0, generalCornerRadius).dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(Icons.Rounded.Opacity, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(maxOf(0, generalCornerRadius).dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text("Mock Content", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                // Simulated NavBar at bottom of preview
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                        .width(220.dp)
                        .height(64.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(maxOf(0, navbarCornerRadius).dp)
                        )
                        .let { if (showOutlines) it.border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(maxOf(0, navbarCornerRadius).dp)) else it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // 1. THEME & COLORS
    item {`;

fs.writeFileSync('app/src/main/java/com/pixelwater/app/ui/AppearanceSettingsSection.kt', data.replace(anchor, replacement), 'utf8');
