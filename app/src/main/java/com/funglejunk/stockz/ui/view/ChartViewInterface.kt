package com.funglejunk.stockz.ui.view

interface ChartViewInterface {

    val pathResetFunc: PathResetFunc
    val animatorInitFunc: AnimatorInitFunc
    val monthMarkersDrawFunc: MonthMarkersDrawFunc
    val yearMarkerDrawFunc: YearMarkersDrawFunc
    val horizontalBarsDrawFunc: HorizontalBarsDrawFunc
    val movingAvDrawFunc: SimpleXyDrawFunc
    val bollingerDrawFunc: DoubleXyDrawFunc
    val atrDrawFunc: SimpleXyDrawFunc

}