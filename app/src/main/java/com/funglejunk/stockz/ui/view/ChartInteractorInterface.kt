package com.funglejunk.stockz.ui.view

import com.funglejunk.stockz.data.DrawableHistoricData

interface ChartInteractorInterface {

    fun prepareDrawing(
        data: DrawableHistoricData,
        viewWidth: Float,
        viewHeight: Float,
        pathResetFunc: PathResetFunc,
        animInit: AnimatorInitFunc,
        monthMarkersDrawFunc: MonthMarkersDrawFunc,
        yearMarkerDrawFunc: YearMarkersDrawFunc,
        horizontalBarsDrawFunc: HorizontalBarsDrawFunc
    ): ChartInteractor.DrawFuncRegister

}