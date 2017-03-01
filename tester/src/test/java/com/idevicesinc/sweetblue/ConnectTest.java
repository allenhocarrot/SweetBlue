package com.idevicesinc.sweetblue;


import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import com.idevicesinc.sweetblue.utils.Pointer;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import static org.junit.Assert.assertFalse;


@Config(manifest = Config.NONE, sdk = 24)
@RunWith(RobolectricTestRunner.class)
public class ConnectTest extends BaseBleUnitTest
{

    private BleDevice m_device;


    @Test(timeout = 6000)
    public void connectCreatedDeviceTest() throws Exception
    {
        m_device = null;

        m_config.loggingEnabled = true;
        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                s.release();
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 6000)
    public void connectDiscoveredDeviceTest() throws Exception
    {
        m_device = null;

        final Semaphore s = new Semaphore(0);

        m_config.defaultScanFilter = new BleManagerConfig.ScanFilter()
        {
            @Override public Please onEvent(ScanEvent e)
            {
                return Please.acknowledgeIf(e.name_native().equals("Test Device"));
            }
        };

        m_config.loggingEnabled = true;
        m_mgr.setConfig(m_config);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                s.release();
                            }
                        }
                    });
                }
            }
        });

        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    UnitTestUtils.advertiseNewDevice(m_mgr, -45, "Test Device");
                }
            }
        });

        m_mgr.startScan();
        s.acquire();
    }

    @Test(timeout = 8000)
    public void connectDiscoveredMultipleDeviceTest() throws Exception
    {
        m_device = null;

        final Semaphore s = new Semaphore(0);

        m_config.defaultScanFilter = new BleManagerConfig.ScanFilter()
        {
            @Override public Please onEvent(ScanEvent e)
            {
                return Please.acknowledgeIf(e.name_native().contains("Test Device"));
            }
        };

        m_config.loggingEnabled = true;
        m_mgr.setConfig(m_config);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            final Pointer<Integer> connected = new Pointer(0);

            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                connected.value++;
                                System.out.println(e.device().getName_override() + " connected.");
                                if (connected.value == 3)
                                {
                                    s.release();
                                }
                            }
                        }
                    });
                }
            }
        });

        m_mgr.setListener_State(new ManagerStateListener()
        {
            @Override public void onEvent(BleManager.StateListener.StateEvent e)
            {
                if (e.didEnter(BleManagerState.SCANNING))
                {
                    UnitTestUtils.advertiseNewDevice(m_mgr, -45, "Test Device #1");
                    UnitTestUtils.advertiseNewDevice(m_mgr, -35, "Test Device #2");
                    UnitTestUtils.advertiseNewDevice(m_mgr, -60, "Test Device #3");
                }
            }
        });

        m_mgr.startScan();
        s.acquire();
    }

    @Test(timeout = 6000)
    public void connectFailTest() throws Exception
    {
        m_device = null;

        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ConnectFailGattLayer(device);
            }
        };
        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                s.release();
                            }
                        }
                    }, new BleDevice.DefaultConnectionFailListener()
                    {
                        @Override public Please onEvent(ConnectionFailEvent e)
                        {
                            System.out.println("Connection fail event: " + e.toString());
                            if (e.failureCountSoFar() == 3)
                            {
                                s.release();
                            }
                            return super.onEvent(e);
                        }
                    } );

                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 15000)
    public void connectThenDisconnectBeforeServiceDiscoveryTest() throws Exception
    {
        m_device = null;

        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new DisconnectBeforeServiceDiscoveryGattLayer(device);
            }
        };

        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                        }
                    }, new BleDevice.DefaultConnectionFailListener() {
                        @Override public Please onEvent(ConnectionFailEvent e)
                        {
                            System.out.println("Connection fail event: " + e.toString());
                            if (e.failureCountSoFar() == 3)
                            {
                                s.release();
                            }
                            return super.onEvent(e);
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 6000)
    public void connectThenFailDiscoverServicesTest() throws Exception
    {
        m_device = null;

        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new DiscoverServicesFailGattLayer(device);
            }
        };

        m_config.connectFailRetryConnectingOverall = true;
        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                        }
                    }, new BleDevice.DefaultConnectionFailListener() {
                        @Override public Please onEvent(ConnectionFailEvent e)
                        {
                            System.out.println("Connection fail event: " + e.toString());
                            if (e.failureCountSoFar() == 3)
                            {
                                s.release();
                            }
                            return super.onEvent(e);
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 45000)
    public void connectThenTimeoutThenFailTest() throws Exception
    {
        m_device = null;

        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new TimeOutGattLayer(device);
            }
        };

        m_config.connectFailRetryConnectingOverall = false;

        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        final BleTransaction.Init init = new BleTransaction.Init()
        {
            @Override protected void start(BleDevice device)
            {
                device.read(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener()
                {
                    @Override public void onEvent(ReadWriteEvent e)
                    {
                        assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                        if (!e.wasSuccess())
                        {
//                            fail();
                        }
                    }
                });
                device.read(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener()
                {
                    @Override public void onEvent(ReadWriteEvent e)
                    {
                        assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                        if (!e.wasSuccess())
                        {
                            fail();
                        }
                    }
                });
            }
        };

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(init, null, new BleDevice.DefaultConnectionFailListener() {
                        @Override public Please onEvent(ConnectionFailEvent e)
                        {
                            System.out.println("Connection fail event: " + e.toString());
                            if (e.failureCountSoFar() == 3)
                            {
                                s.release();
                            }
                            return super.onEvent(e);
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();

    }

    @Test(timeout = 7000)
    public void connectThenFailInitTxnTest() throws Exception
    {
        m_device = null;

        m_config.loggingEnabled = true;
        m_config.gattLayerFactory = new P_GattLayerFactory()
        {
            @Override public P_GattLayer newInstance(BleDevice device)
            {
                return new ReadFailGattLayer(device);
            }
        };
        m_config.connectFailRetryConnectingOverall = true;
        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        final BleTransaction.Init init = new BleTransaction.Init()
        {
            @Override protected void start(BleDevice device)
            {
                device.read(Uuids.BATTERY_SERVICE_UUID, Uuids.BATTERY_LEVEL, new BleDevice.ReadWriteListener()
                {
                    @Override public void onEvent(ReadWriteEvent e)
                    {
                        assertFalse("Read was successful! How did this happen?", e.wasSuccess());
                        if (!e.wasSuccess())
                        {
                            fail();
                        }
                    }
                });
            }
        };

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {
            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(init, null, new BleDevice.DefaultConnectionFailListener() {
                        @Override public Please onEvent(ConnectionFailEvent e)
                        {
                            System.out.println("Connection fail event: " + e.toString());
                            if (e.failureCountSoFar() == 3)
                            {
                                s.release();
                            }
                            return super.onEvent(e);
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }

    @Test(timeout = 6000)
    public void connectThenDisconnectTest() throws Exception
    {
        m_device = null;

        m_config.loggingEnabled = true;
        m_mgr.setConfig(m_config);

        final Semaphore s = new Semaphore(0);

        m_mgr.setListener_Discovery(new BleManager.DiscoveryListener()
        {

            boolean hasConnected = false;

            @Override public void onEvent(DiscoveryEvent e)
            {
                if (e.was(LifeCycle.DISCOVERED))
                {
                    m_device = e.device();
                    m_device.connect(new BleDevice.StateListener()
                    {
                        @Override public void onEvent(StateEvent e)
                        {
                            if (e.didEnter(BleDeviceState.INITIALIZED))
                            {
                                hasConnected = true;
                                m_device.disconnect();
                                UnitTestUtils.disconnectDevice(m_device, BleStatuses.GATT_SUCCESS);
                            }
                            else if (hasConnected && e.didEnter(BleDeviceState.DISCONNECTED))
                            {
                                s.release();
                            }
                        }
                    });
                }
            }
        });

        m_mgr.newDevice(UnitTestUtils.randomMacAddress(), "Test Device");

        s.acquire();
    }



    @Override public BleManagerConfig getConfig()
    {
        m_config = new BleManagerConfig();
        m_config.nativeManagerLayer = new UnitTestManagerLayer();
        m_config.nativeDeviceFactory = new P_NativeDeviceLayerFactory<UnitTestDevice>()
        {
            @Override public UnitTestDevice newInstance(BleDevice device)
            {
                return new UnitTestDevice(device);
            }
        };
        m_config.gattLayerFactory = new P_GattLayerFactory<UnitTestGatt>()
        {
            @Override public UnitTestGatt newInstance(BleDevice device)
            {
                return new UnitTestGatt(device);
            }
        };
        m_config.logger = new UnitTestLogger();
        m_config.runOnMainThread = false;
        return m_config;
    }

    private class TimeOutGattLayer extends UnitTestGatt
    {

        public TimeOutGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public List<BluetoothGattService> getNativeServiceList(P_Logger logger)
        {
            List<BluetoothGattService> list = new ArrayList<>();
            BluetoothGattService service = new BluetoothGattService(Uuids.BATTERY_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            BluetoothGattCharacteristic ch = new BluetoothGattCharacteristic(Uuids.BATTERY_LEVEL, BleCharacteristicProperty.READ.bit(), BleCharacteristicPermission.READ.bit());
            service.addCharacteristic(ch);
            return list;
        }

        @Override public BluetoothGattService getService(UUID serviceUuid, P_Logger logger)
        {
            if (serviceUuid.equals(Uuids.BATTERY_SERVICE_UUID))
            {
                BluetoothGattService service = new BluetoothGattService(Uuids.BATTERY_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
                BluetoothGattCharacteristic ch = new BluetoothGattCharacteristic(Uuids.BATTERY_LEVEL, BleCharacteristicProperty.READ.bit(), BleCharacteristicPermission.READ.bit());
                service.addCharacteristic(ch);
                return service;
            }
            else
            {
                return null;
            }
        }

        @Override public boolean readCharacteristic(final BluetoothGattCharacteristic characteristic)
        {
            UnitTestUtils.disconnectDevice(getBleDevice(), BleStatuses.GATT_ERROR, 14500);
            return true;
        }
    }

    private class ReadFailGattLayer extends UnitTestGatt
    {

        public ReadFailGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public List<BluetoothGattService> getNativeServiceList(P_Logger logger)
        {
            List<BluetoothGattService> list = new ArrayList<>();
            BluetoothGattService service = new BluetoothGattService(Uuids.BATTERY_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            BluetoothGattCharacteristic ch = new BluetoothGattCharacteristic(Uuids.BATTERY_LEVEL, BleCharacteristicProperty.READ.bit(), BleCharacteristicPermission.READ.bit());
            service.addCharacteristic(ch);
            return list;
        }

        @Override public BluetoothGattService getService(UUID serviceUuid, P_Logger logger)
        {
            if (serviceUuid.equals(Uuids.BATTERY_SERVICE_UUID))
            {
                BluetoothGattService service = new BluetoothGattService(Uuids.BATTERY_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
                BluetoothGattCharacteristic ch = new BluetoothGattCharacteristic(Uuids.BATTERY_LEVEL, BleCharacteristicProperty.READ.bit(), BleCharacteristicPermission.READ.bit());
                service.addCharacteristic(ch);
                return service;
            }
            else
            {
                return null;
            }
        }

        @Override public boolean readCharacteristic(final BluetoothGattCharacteristic characteristic)
        {
            UnitTestUtils.readError(getBleDevice(), characteristic, BleStatuses.GATT_ERROR, 150);
            return true;
        }
    }

    private class ConnectFailGattLayer extends UnitTestGatt
    {

        public ConnectFailGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public void setToConnecting()
        {
            super.setToConnecting();
            UnitTestUtils.disconnectDevice(getBleDevice(), BleStatuses.GATT_ERROR, 50);
        }

        @Override public void setToConnected()
        {
        }
    }

    private class DisconnectBeforeServiceDiscoveryGattLayer extends UnitTestGatt
    {

        public DisconnectBeforeServiceDiscoveryGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public BluetoothGatt connect(P_NativeDeviceLayer device, Context context, boolean useAutoConnect, BluetoothGattCallback callback)
        {
            UnitTestUtils.disconnectDevice(getBleDevice(), BleStatuses.GATT_ERROR, false, 175);
            return super.connect(device, context, useAutoConnect, callback);
        }
    }

    private class DiscoverServicesFailGattLayer extends UnitTestGatt
    {

        public DiscoverServicesFailGattLayer(BleDevice device)
        {
            super(device);
        }

        @Override public void setServicesDiscovered()
        {
            UnitTestUtils.failDiscoverServices(getBleDevice(), BleStatuses.GATT_STATUS_NOT_APPLICABLE);
        }
    }

}
