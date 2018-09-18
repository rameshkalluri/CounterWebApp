package com.rccl.core.product.service.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.rccl.core.constants.RcclcoreConstants;
import com.rccl.core.model.InventoryBlockModel;
import com.rccl.core.model.OfferingModel;
import com.rccl.core.product.dao.RcclStockDao;
import com.rccl.core.product.service.RcclStockService;
import com.rccl.core.stock.data.BlockTypeStrategyParam;
import com.rccl.core.stock.dto.InventoryDto;
import com.rccl.core.stock.service.RcclBlockTypeListBeanPostProcessor;
import com.rccl.core.stock.service.RcclInventoryBlockService;
import com.rccl.core.strategy.RcclCommerceAvailabilityCalculationStrategy;
import com.rccl.core.strategy.RcclStockLevelStatusStrategy;
import com.rccl.core.util.RcclUtils;
import de.hybris.platform.basecommerce.enums.InStockStatus;
import de.hybris.platform.basecommerce.enums.StockLevelStatus;
import de.hybris.platform.core.model.order.AbstractOrderEntryModel;
import de.hybris.platform.core.model.order.OrderModel;
import de.hybris.platform.core.model.product.ProductModel;
import de.hybris.platform.ordercancel.OrderCancelEntry;
import de.hybris.platform.orderhistory.model.OrderHistoryEntryModel;
import de.hybris.platform.ordersplitting.model.StockLevelModel;
import de.hybris.platform.servicelayer.interceptor.impl.InterceptorExecutionPolicy;
import de.hybris.platform.servicelayer.internal.dao.GenericDao;
import de.hybris.platform.servicelayer.session.SessionExecutionBody;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.time.TimeService;
import de.hybris.platform.store.BaseStoreModel;
import de.hybris.platform.travelservices.model.user.TravellerModel;
import de.hybris.platform.travelservices.model.warehouse.TransportOfferingModel;
import de.hybris.platform.travelservices.stock.impl.DefaultTravelCommerceStockService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

import static de.hybris.platform.servicelayer.util.ServicesUtil.validateParameterNotNull;


/**
 * default implementation for {@link RcclStockService}
 *
 * @see #getStockLevelByItemCode(String)
 * @see #calculateInventories(BlockTypeStrategyParam, ProductModel, List)
 * @see #calculateInventory(BlockTypeStrategyParam, StockLevelModel)
 * @see #getStocklevels(String, String)
 * @see #checkStatus(InventoryBlockModel)
 * @see #calculateAvailability(InventoryBlockModel)
 * @see #getStockLevelForItemCode(String)
 * @see #getOfferingByStartDate(String, Date, String)
 * @see #getInventory(BlockTypeStrategyParam, ProductModel, List)
 * @see #getInventory(BlockTypeStrategyParam, StockLevelModel)
 * @see #getStockLevelsForProduct(ProductModel, List, boolean)
 * @see #getOfferingByCode(String)
 * @see #isValidOffering(OfferingModel)
 * @see #createInventoryFetchKey(TravellerModel, ProductModel, StockLevelModel)
 * @see #fetchInventory(TravellerModel, StockLevelModel, BaseStoreModel)
 */
public class DefaultRcclStockService extends DefaultTravelCommerceStockService implements RcclStockService{

    private static final Logger LOG = Logger.getLogger(DefaultRcclStockService.class);
    private static final String NOT_BLANK_ERR_MSG = "itemCode can't be blank";
    private static final String RCCL_STOCK_LEVEL_INTERCEPTOR = "rcclStockLevelInterceptor";
    private static final String RCCL_INVENTORY_BLOCK_INTERCEPTOR = "rcclInventoryBlockInterceptor";

    private RcclStockDao rcclStockDao;
    private RcclBlockTypeListBeanPostProcessor blockTypeListBeanPostProcessor;
    private RcclCommerceAvailabilityCalculationStrategy availableStrategy;
    private RcclStockLevelStatusStrategy statusStrategy;
    private GenericDao<StockLevelModel> stockLevelGenericDao;
    private TimeService timeService;
    private RcclInventoryBlockService rcclInventoryBlockService;
    private SessionService sessionService;

    /**
     * find the {@link StockLevelModel} by {@link StockLevelModel#ITEMCODE}
     *
     * @param itemCode the itemCode of stockLevel
     * @return the {@link StockLevelModel}
     */
    @Override
    public StockLevelModel getStockLevelByItemCode(final String itemCode){

        if (StringUtils.isBlank(itemCode)) {
            throw new IllegalArgumentException(NOT_BLANK_ERR_MSG);
        }
        final Map<String, ? super Object> params = new HashMap<>();
        params.put(StockLevelModel.ITEMCODE, itemCode);
        final List<StockLevelModel> stockLevels = getStockLevelGenericDao().find(params);
        if (CollectionUtils.isNotEmpty(stockLevels)) {
            return stockLevels.get(0);
        }
        return null;
    }
    
    @Override
    public StockLevelModel getStockLevelById(final String id){

        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException(NOT_BLANK_ERR_MSG);
        }
        final Map<String, ? super Object> params = new HashMap<>();
        params.put(StockLevelModel.ID, id);
        final List<StockLevelModel> stockLevels = getStockLevelGenericDao().find(params);
        if (CollectionUtils.isNotEmpty(stockLevels)) {
            return stockLevels.get(0);
        }
        return null;
    }

    /**
     * @param blockTypeStrategyParam the {@link BlockTypeStrategyParam}
     * @param productModel           the {@link ProductModel}
     * @param transportOfferings     the {@link List} of {@link TransportOfferingModel}
     * @return the {@link List} of {@link InventoryDto}
     */

    @Override
    public List<InventoryDto> calculateInventories(final BlockTypeStrategyParam blockTypeStrategyParam, final ProductModel productModel,
                    final List<TransportOfferingModel> transportOfferings){

        final List<InventoryBlockModel> inventoryBlocks = getInventory(blockTypeStrategyParam, productModel, transportOfferings);
        if (CollectionUtils.isNotEmpty(inventoryBlocks)) {
            final boolean includeSoldOutOfferings =
                            BooleanUtils.isTrue(blockTypeStrategyParam.getValue(RcclcoreConstants.INCLUDE_PAST_OFFERINGS));
            return inventoryBlocks.stream().map(inventoryBlock -> getInventoryDto(includeSoldOutOfferings, inventoryBlock))
                            .collect(Collectors.toList());
        }
        return ListUtils.EMPTY_LIST;

    }


    /**
     * Executes {@link #getInventory(BlockTypeStrategyParam, StockLevelModel)} and
     * gets {@link InventoryBlockModel}. Then converts {@link InventoryBlockModel}
     * to {@link InventoryDto} and sets availableQty, stockStatus,
     * inventoryAvailable
     *
     * @param blockTypeStrategyParam the {@link BlockTypeStrategyParam}
     * @param stockLevelModel        the {@link StockLevelModel}
     * @return the {@link InventoryDto}
     */
    @Override
    public InventoryDto calculateInventory(final BlockTypeStrategyParam blockTypeStrategyParam, final StockLevelModel stockLevelModel){

        final InventoryBlockModel inventoryBlock = getInventory(blockTypeStrategyParam, stockLevelModel);
        return getInventoryDto(BooleanUtils.isTrue(blockTypeStrategyParam.getValue(RcclcoreConstants.INCLUDE_PAST_OFFERINGS)),
                        inventoryBlock);
    }


    /*
     * (non-Javadoc)
     *
     * @see com.rccl.core.product.service.RCCLStockService#getStocklevel(java.lang.
     * String, java.lang.String)
     */
    @Override
    public List<StockLevelModel> getStocklevels(final String productCode, final String sailingCode){

        return getRcclStockDao().getStocklevels(productCode, sailingCode);
    }

    /**
     * Gets the offering by start date.
     *
     * @param productCode           the product code
     * @param offeringTime          the start date time
     * @param transportofferingcode the transportofferingcode
     * @return the offering by start date
     */
    @Override
    public List<OfferingModel> getOfferingByStartDate(final String productCode, final Date offeringTime,
                    final String transportofferingcode){

        return getRcclStockDao().getOfferingByStartDate(productCode, offeringTime, transportofferingcode);
    }

    /**
     * This method returns all the stock or offerings for a product and a list of
     * transportoffeings. includePastOfferings if true, returns the past offerings.
     * if false, return offering from current time.
     */
    @Override
    public List<StockLevelModel> getStockLevelsForProduct(final ProductModel productModel,
                    final List<TransportOfferingModel> transportOfferings, final boolean includePastOfferings){

        if (CollectionUtils.isNotEmpty(transportOfferings)) {
            return getRcclStockDao()
                            .findStockLevelsForProduct(productModel.getCode(), transportOfferings, RcclUtils.isServiceProduct(productModel),
                                            includePastOfferings);
        }
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.rccl.core.product.service.RcclStockService#getInventory(com.rccl.core
     * .stock.data.BlockTypeStrategyParam,
     * de.hybris.platform.core.model.product.ProductModel, java.util.List)
     *
     * mapParam holds all the prerequisites needed for filtering. Like Base store,
     * includePastOfferings from request.
     *
     * @return List<InventoryBlockModel> for a specified product and sailing.
     */

    @Override
    public List<InventoryBlockModel> getInventory(final BlockTypeStrategyParam mapParam, final ProductModel productModel,
                    final List<TransportOfferingModel> transportOfferings){

        final List<StockLevelModel> stocklevels = getStockLevelsForProduct(productModel, transportOfferings,
                        BooleanUtils.isTrue(mapParam.getValue(RcclcoreConstants.INCLUDE_PAST_OFFERINGS)));
        final List<InventoryBlockModel> inventoryBlocks = new ArrayList<>();
        for (final StockLevelModel stockLevelModel : stocklevels) {
            final InventoryBlockModel inventoryBlock;
            inventoryBlock = getInventory(mapParam, stockLevelModel);
            inventoryBlocks.add(inventoryBlock);
        }
        return inventoryBlocks;
    }

    /*
     * Gets the offering by offering code
     *
     * @see
     * com.rccl.core.product.service.RcclStockService#getOfferingByCode(java.lang.
     * String)
     */
    @Override
    public OfferingModel getOfferingByCode(final String offeringCode){
        return getRcclStockDao().getOfferingByCode(offeringCode);
    }
    
    @Override
    public OfferingModel getOfferingById(final String offeringId){
        return getRcclStockDao().getOfferingById(offeringId);
    }

    /**
     * validates the Offering is valid at current time ot not
     *
     * @param offeringModel the {@link OfferingModel}
     * @return tru or false
     */
    @Override
    public boolean isValidOffering(final OfferingModel offeringModel){

        return offeringModel != null && offeringModel.isActive() && getTimeService().getCurrentTime()
                        .before(offeringModel.getOfferingTime());
    }

    @Override
    public InventoryBlockModel getInventory(final BlockTypeStrategyParam blockTypeStrategyParam, final StockLevelModel stockLevelModel){

        if (InStockStatus.NOTSPECIFIED.equals(stockLevelModel.getInStockStatus()) && stockLevelModel.getInventoryBlocks() != null) {
            blockTypeStrategyParam.putValue(BlockTypeStrategyParam.INVENTROY_BLOCKS, stockLevelModel.getInventoryBlocks());
            return getBlockTypeListBeanPostProcessor().getInventoryBlock(blockTypeStrategyParam);
        } else {
            final InventoryBlockModel inventoryBlock = new InventoryBlockModel();
            //inventoryBlock.setCode(stockLevelModel.getItemCode());
            inventoryBlock.setCode(stockLevelModel.getId());
            inventoryBlock.setStocklevel(stockLevelModel);
            return inventoryBlock;
        }
    }

    /**
     * creates the inventory fetch key according to the inventory retrival critaria.
     * Helps avoiding redundant retrieval execution for same critarias.
     *
     * @param travellerModel  the {@link TravellerModel}
     * @param stockLevelModel the {@link StockLevelModel}
     * @param productModel    the {@link ProductModel}
     * @return the Key
     */
    @Override
    public String createInventoryFetchKey(final TravellerModel travellerModel, final @Nonnull ProductModel productModel,
                    final StockLevelModel stockLevelModel){

        /*
         * TODO : to be enhanced after the complete implementation of
         * inventoryBlockStrategy
         */
        if (stockLevelModel != null) {
            return stockLevelModel.getItemCode();
        } else {
            return productModel.getCode();
        }
    }


    /**
     * calculates Inventory. Uses
     * {@link RcclStockService#getInventory(BlockTypeStrategyParam, StockLevelModel)}
     *
     * @param travellerModel  the {@link TravellerModel}
     * @param stockLevelModel the {@link StockLevelModel}
     * @param baseStore       the {@link BaseStoreModel} current BaseStore
     * @return the {@link InventoryDto}
     */
    @Override
    public InventoryDto fetchInventory(final TravellerModel travellerModel, final StockLevelModel stockLevelModel,
                    final BaseStoreModel baseStore){

        final BlockTypeStrategyParam blockTypeStrategyParam = new BlockTypeStrategyParam();
        blockTypeStrategyParam.putValue(BlockTypeStrategyParam.BASESTORE, baseStore);
        blockTypeStrategyParam.putValue(BlockTypeStrategyParam.TRAVELLER, travellerModel);
        return calculateInventory(blockTypeStrategyParam, stockLevelModel);
    }

    /**
     * convert the {@link InventoryBlockModel} to {@link InventoryDto} calculate
     * availableQty, stockStatus, inventoryAvailable and set in {@link InventoryDto}
     *
     * @param includeSoldOutOfferings the boolean
     * @param inventoryBlock          the {@link InventoryBlockModel}
     * @return the {@link InventoryDto}
     */
    private InventoryDto getInventoryDto(final boolean includeSoldOutOfferings, final InventoryBlockModel inventoryBlock){

        final InventoryDto inventoryDto = new InventoryDto();
        final StockLevelModel stockLevelModel = inventoryBlock.getStocklevel();
        inventoryDto.setForceInStock(InStockStatus.FORCEINSTOCK.equals(stockLevelModel.getInStockStatus()));
        inventoryDto.setInventoryBlock(inventoryBlock);
        inventoryDto.setStockLevel(stockLevelModel);
        inventoryDto.setStockLevelStatus(checkStatus(inventoryBlock));
        inventoryDto.setExpired(!isValidStockLevel(stockLevelModel));
        final Long availableQty = calculateAvailability(inventoryBlock);
        inventoryDto.setAvailableQty(availableQty);
        if (includeSoldOutOfferings) {
            inventoryDto.setInventoryAvailable(StockLevelStatus.INSTOCK.equals(inventoryDto.getStockLevelStatus()) || (availableQty != null
                            && availableQty > 0));
        }
        return inventoryDto;
    }

    /**
     * @param stockLevelModel the {@link StockLevelModel}
     * @return if the stockLevelModel is valid or not
     */
    private boolean isValidStockLevel(final StockLevelModel stockLevelModel){

        return stockLevelModel != null && stockLevelModel.isActive() && (!(stockLevelModel instanceof OfferingModel) || isValidOffering(
                        (OfferingModel) stockLevelModel));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.rccl.core.product.service.RcclStockService#checkStatus(com.rccl.core.
     * model.InventoryBlockModel)
     */
    @Override
    public StockLevelStatus checkStatus(final InventoryBlockModel inventoryBlockModel){

        // YTODO Auto-generated method stub
        return getStatusStrategy().checkStatus(inventoryBlockModel);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.rccl.core.product.service.RcclStockService#calculateAvailability(com.
     * rccl.core.model.InventoryBlockModel)
     */
    @Override
    public Long calculateAvailability(final InventoryBlockModel inventoryBlockModel){

        // YTODO Auto-generated method stub
        return getAvailableStrategy().calculateAvailability(inventoryBlockModel);
    }

    /**
     * Gets the stock level for item code.
     *
     * @param itemCode the item code
     * @return the stock level for item code
     */

    @Override
    public StockLevelModel getStockLevelForItemCode(final String itemCode){

        final List<StockLevelModel> stockLevelModel = getRcclStockDao().getStockLevelByItemcode(itemCode);
        return CollectionUtils.isNotEmpty(stockLevelModel) ? stockLevelModel.get(0) : null;
    }
    
    public StockLevelModel getStockLevelForID(final String id){
        final List<StockLevelModel> stockLevelModel = getRcclStockDao().getStockLevelByID(id);
        return CollectionUtils.isNotEmpty(stockLevelModel) ? stockLevelModel.get(0) : null;
    }    

    /**
     * This method is used to release the order entries from the respective inventory blocks
     * Based on the quantity of order.
     *
     * @param orderEntries
     * @see #releaseInternal(String, int)
     */
    @Override
    public void releaseInventory(final List<OrderCancelEntry> orderEntries){

        validateParameterNotNull(orderEntries, "orderEntries cannot be null");
        orderEntries.stream().forEach(orderCancelEntry ->{
            final AbstractOrderEntryModel orderEntry = orderCancelEntry.getOrderEntry();
            LOG.info(String.format("Cancelling %s Order Entry in Order %s", orderEntry.getEntryNumber(), orderEntry.getOrder().getCode()));
            releaseInternal(orderEntry.getInventoryBlock(), (int)orderCancelEntry.getCancelQuantity());
        });
    }

    /**
     * This method is used to release the orders from Inventory Block based on inventoryBlockId and qtyToBeReleased.
     *
     * @param inventoryBlockId
     * @param qtyToBeReleased
     */
    private void releaseInternal(final String inventoryBlockId, final int qtyToBeReleased){

        if (qtyToBeReleased <= 0) {
            LOG.error("Quantity to be released must be greater than zero.");
            return;
        }

        Optional<InventoryBlockModel> inventoryBlockModel =
                        Optional.ofNullable(getRcclInventoryBlockService().getInventoryBlockByCode(inventoryBlockId));
        inventoryBlockModel.ifPresent(inventoryBlock -> {
            final Map<String, Object> params = ImmutableMap.of(InterceptorExecutionPolicy.DISABLED_INTERCEPTOR_BEANS,
                            ImmutableSet.of(RCCL_STOCK_LEVEL_INTERCEPTOR, RCCL_INVENTORY_BLOCK_INTERCEPTOR));

            getSessionService().executeInLocalViewWithParams(params, new SessionExecutionBody(){

                @Override
                public void executeWithoutResult(){

                    int reservedInSystem = inventoryBlock.getReserved();
                    if (reservedInSystem >= qtyToBeReleased) {
                        reservedInSystem = reservedInSystem - qtyToBeReleased;
                    } else {
                        reservedInSystem = qtyToBeReleased;
                    }
                    inventoryBlock.setReserved(reservedInSystem);
                    getModelService().save(inventoryBlock);
                    getModelService().refresh(inventoryBlock);
                }
            });

            //TODO : add entries as how maintained to check the reason for release in inventory with comment

        });
    }


    /**
     * @return the availableStrategy
     */

    public RcclCommerceAvailabilityCalculationStrategy getAvailableStrategy(){

        return availableStrategy;
    }

    /**
     * @param availableStrategy the availableStrategy to set
     */
    @Required
    public void setAvailableStrategy(final RcclCommerceAvailabilityCalculationStrategy availableStrategy){

        this.availableStrategy = availableStrategy;
    }

    /**
     * @return the statusStrategy
     */
    public RcclStockLevelStatusStrategy getStatusStrategy(){

        return statusStrategy;
    }

    /**
     * @param statusStrategy the statusStrategy to set
     */
    @Required
    public void setStatusStrategy(final RcclStockLevelStatusStrategy statusStrategy){

        this.statusStrategy = statusStrategy;
    }

    public GenericDao<StockLevelModel> getStockLevelGenericDao(){

        return stockLevelGenericDao;
    }

    @Required
    public void setStockLevelGenericDao(final GenericDao<StockLevelModel> stockLevelGenericDao){

        this.stockLevelGenericDao = stockLevelGenericDao;
    }

    protected TimeService getTimeService(){

        return timeService;
    }

    @Required
    public void setTimeService(final TimeService timeService){

        this.timeService = timeService;
    }

    public RcclStockDao getRcclStockDao(){

        return rcclStockDao;
    }

    @Required
    public void setRcclStockDao(final RcclStockDao rcclStockDao){

        this.rcclStockDao = rcclStockDao;
    }

    public RcclBlockTypeListBeanPostProcessor getBlockTypeListBeanPostProcessor(){

        return blockTypeListBeanPostProcessor;
    }


    @Required
    public void setBlockTypeListBeanPostProcessor(final RcclBlockTypeListBeanPostProcessor blockTypeListBeanPostProcessor){

        this.blockTypeListBeanPostProcessor = blockTypeListBeanPostProcessor;
    }

    public RcclInventoryBlockService getRcclInventoryBlockService(){

        return rcclInventoryBlockService;
    }

    @Required
    public void setRcclInventoryBlockService(RcclInventoryBlockService rcclInventoryBlockService){

        this.rcclInventoryBlockService = rcclInventoryBlockService;
    }

    public SessionService getSessionService(){

        return sessionService;
    }

    @Required
    public void setSessionService(SessionService sessionService){

        this.sessionService = sessionService;
    }
    
    /**
     * Gets the offering by sailing and product code.
     *
     * @param productCode the product code
     * @param sailing the start date time
     * @return the offering by product code and sailing
     */
    @Override
    public List<OfferingModel> getOfferingBySailingAndProductCode(final String productCode, final String sailing){

    	return getRcclStockDao().getOfferingBySailingAndProductCode(productCode, sailing);
    }
}
