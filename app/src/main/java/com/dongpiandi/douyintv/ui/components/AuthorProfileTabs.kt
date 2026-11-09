package com.dongpiandi.douyintv.ui.components

internal fun shouldShowAuthorCollectionsTab(
    collectionsLoaded: Boolean,
    collectionCount: Int,
): Boolean = !collectionsLoaded || collectionCount > 0
